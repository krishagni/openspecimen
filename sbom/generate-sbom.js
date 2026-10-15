#!/usr/bin/env node

/* Generates a CycloneDX 1.6 SBOM from the OpenSpecimen Gradle and npm manifests. */
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');

const coreDir = path.resolve(__dirname, '..');
const pluginsDir = process.argv[2] || path.resolve(coreDir, '..', 'openspecimen-plugins');
const output = process.argv[3] || path.join(__dirname, 'openspecimen.cdx.json');

function read(file) {
  return fs.readFileSync(file, 'utf8');
}

function git(dir, args) {
  return childProcess.execFileSync('git', args, { cwd: dir, encoding: 'utf8' }).trim();
}

function encode(value) {
  return encodeURIComponent(value).replace(/%2F/gi, '/');
}

function mavenRef(group, name, version) {
  return `pkg:maven/${encode(group)}/${encode(name)}@${encode(version)}`;
}

function npmRef(name, version) {
  const slash = name.lastIndexOf('/');
  const encodedName = slash >= 0
    ? `${encode(name.slice(0, slash))}/${encode(name.slice(slash + 1))}`
    : encode(name);
  return `pkg:npm/${encodedName}@${encode(version)}`;
}

function parseGradleDependencies(file) {
  const text = read(file);
  const dependencies = [];
  const accepted = '(?:implementation|runtimeOnly|providedCompile)';
  const map = new Map();
  const variablePattern = /def\s+(\w+)\s*=\s*["']([^"']+)["']/g;
  for (const match of text.matchAll(variablePattern)) {
    map.set(match[1], match[2]);
  }

  const longPattern = new RegExp(
    `\\b(${accepted})\\s+group:\\s*["']([^"']+)["']\\s*,\\s*name:\\s*["']([^"']+)["']\\s*,\\s*version:\\s*(?:["']([^"']+)["']|(\\w+))`,
    'g'
  );
  for (const match of text.matchAll(longPattern)) {
    const version = match[4] || map.get(match[5]);
    if (version) dependencies.push({ group: match[2], name: match[3], version });
  }

  const shortPattern = new RegExp(`\\b(${accepted})\\s+["']([^:'"]+):([^:'"]+):([^'"]+)["']`, 'g');
  for (const match of text.matchAll(shortPattern)) {
    dependencies.push({ group: match[2], name: match[3], version: match[4] });
  }
  return dependencies;
}

function componentKey(component) {
  return component.purl || component['bom-ref'];
}

function uniqueComponents(components) {
  return [...new Map(components.map(component => [componentKey(component), component])).values()];
}

function mavenComponent(dependency, properties = []) {
  const purl = mavenRef(dependency.group, dependency.name, dependency.version);
  const component = {
    type: 'library',
    group: dependency.group,
    name: dependency.name,
    version: dependency.version,
    'bom-ref': purl,
    purl
  };
  if (properties.length) component.properties = properties;
  return component;
}

function localJarComponent(file) {
  const name = path.basename(file, '.jar');
  const versionMatch = name.match(/-(\d[\w.+-]*)$/);
  const version = versionMatch ? versionMatch[1] : 'unknown';
  const artifact = versionMatch ? name.slice(0, -versionMatch[0].length) : name;
  const hash = crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
  const ref = `urn:openspecimen:jar:${artifact}:${version}`;
  return {
    type: 'library',
    name: artifact,
    version,
    'bom-ref': ref,
    hashes: [{ alg: 'SHA-256', content: hash }],
    properties: [{ name: 'openspecimen:source-path', value: path.relative(coreDir, file) }]
  };
}

const coreCommit = git(coreDir, ['rev-parse', 'HEAD']);
const coreVersion = git(coreDir, ['branch', '--show-current']) || coreCommit.slice(0, 12);
const pluginsCommit = git(pluginsDir, ['rev-parse', 'HEAD']);
const pluginsVersion = git(pluginsDir, ['branch', '--show-current']) || pluginsCommit.slice(0, 12);
const rootRef = `pkg:generic/openspecimen-distribution@${encode(coreVersion)}`;
const coreRef = `pkg:maven/com.krishagni/openspecimen@${encode(coreVersion)}`;

const coreMavenDeps = parseGradleDependencies(path.join(coreDir, 'build.gradle'));
const coreMavenNames = new Set(coreMavenDeps.map(dep => `${dep.group}:${dep.name}`));
let coreComponents = coreMavenDeps.map(dep => mavenComponent(dep));

for (const filename of fs.readdirSync(path.join(coreDir, 'lib'))) {
  if (filename.endsWith('.jar')) coreComponents.push(localJarComponent(path.join(coreDir, 'lib', filename)));
}

const lock = JSON.parse(read(path.join(coreDir, 'ui', 'package-lock.json')));
const npmComponents = [];
for (const [location, details] of Object.entries(lock.packages || {})) {
  if (!location || !location.includes('node_modules/') || details.dev || !details.version) continue;
  const name = location.slice(location.lastIndexOf('node_modules/') + 'node_modules/'.length);
  const purl = npmRef(name, details.version);
  const component = {
    type: 'library', name, version: details.version, 'bom-ref': purl, purl,
    properties: [{ name: 'openspecimen:source-manifest', value: 'ui/package-lock.json' }]
  };
  if (details.integrity && details.integrity.startsWith('sha512-')) {
    component.hashes = [{ alg: 'SHA-512', content: Buffer.from(details.integrity.slice(7), 'base64').toString('hex') }];
  }
  npmComponents.push(component);
}
coreComponents = uniqueComponents([...coreComponents, ...npmComponents]);

const includedProjects = read(path.join(pluginsDir, 'settings.gradle'))
  .split('\n')
  .map(line => line.match(/^include\s+['"]:([^'"]+)['"]/))
  .filter(Boolean)
  .map(match => match[1]);
const sharedPluginDeps = parseGradleDependencies(path.join(pluginsDir, 'build.gradle'));
const pluginComponents = [];
const pluginDependencies = [];
const pluginRefs = new Map();

for (const project of includedProjects) {
  const propertiesFile = path.join(pluginsDir, project, 'gradle.properties');
  const props = Object.fromEntries(read(propertiesFile).split(/\r?\n/).filter(Boolean).map(line => line.split(/=(.*)/s).slice(0, 2)));
  const pluginName = props.osPluginJarName || project;
  const ref = `pkg:maven/com.krishagni/${encode(pluginName)}@${encode(pluginsVersion)}`;
  pluginRefs.set(project, ref);
  pluginComponents.push({
    type: 'application',
    group: 'com.krishagni',
    name: pluginName,
    version: pluginsVersion,
    'bom-ref': ref,
    purl: ref,
    scope: 'optional',
    properties: [
      { name: 'openspecimen:plugin-project', value: project },
      { name: 'openspecimen:repository-commit', value: pluginsCommit }
    ]
  });
}

const pluginOnlyLibraryComponents = [];
for (const project of includedProjects) {
  const buildFile = path.join(pluginsDir, project, 'build.gradle');
  const projectText = fs.existsSync(buildFile) ? read(buildFile) : '';
  const declared = fs.existsSync(buildFile) ? parseGradleDependencies(buildFile) : [];
  const unique = [...sharedPluginDeps, ...declared]
    .filter(dep => !coreMavenNames.has(`${dep.group}:${dep.name}`));
  const refs = unique.map(dep => mavenRef(dep.group, dep.name, dep.version));
  pluginOnlyLibraryComponents.push(...unique.map(dep => mavenComponent(dep, [
    { name: 'openspecimen:plugin-only', value: 'true' }
  ])));
  for (const match of projectText.matchAll(/implementation\s+project\(['"]:([^'"]+)['"]\)/g)) {
    if (pluginRefs.has(match[1])) refs.push(pluginRefs.get(match[1]));
  }
  pluginDependencies.push({ ref: pluginRefs.get(project), dependsOn: [...new Set(refs)].sort() });
}

const components = uniqueComponents([
  {
    type: 'application', group: 'com.krishagni', name: 'openspecimen', version: coreVersion,
    'bom-ref': coreRef, purl: coreRef,
    properties: [{ name: 'openspecimen:repository-commit', value: coreCommit }]
  },
  ...coreComponents,
  ...pluginComponents,
  ...pluginOnlyLibraryComponents
]).sort((a, b) => a['bom-ref'].localeCompare(b['bom-ref']));

const seed = crypto.createHash('sha256').update(`${coreCommit}:${pluginsCommit}`).digest('hex');
const uuid = `${seed.slice(0, 8)}-${seed.slice(8, 12)}-4${seed.slice(13, 16)}-a${seed.slice(17, 20)}-${seed.slice(20, 32)}`;
const bom = {
  bomFormat: 'CycloneDX',
  specVersion: '1.6',
  serialNumber: `urn:uuid:${uuid}`,
  version: 1,
  metadata: {
    timestamp: new Date().toISOString(),
    tools: { components: [{ type: 'application', name: 'OpenSpecimen SBOM generator', version: '1.0.0' }] },
    component: {
      type: 'application', name: 'OpenSpecimen distribution', version: coreVersion, 'bom-ref': rootRef,
      properties: [
        { name: 'openspecimen:core-commit', value: coreCommit },
        { name: 'openspecimen:plugins-commit', value: pluginsCommit },
        { name: 'openspecimen:plugin-policy', value: 'Optional plugins list only dependencies absent from the core application by package identity.' },
        { name: 'openspecimen:excluded-scope', value: 'Test and build-only dependencies, deprecated www directories, and development-only npm packages.' }
      ]
    }
  },
  components,
  dependencies: [
    { ref: rootRef, dependsOn: [coreRef] },
    { ref: coreRef, dependsOn: coreComponents.map(componentKey).sort() },
    ...pluginDependencies.sort((a, b) => a.ref.localeCompare(b.ref))
  ]
};

fs.writeFileSync(output, `${JSON.stringify(bom, null, 2)}\n`);
console.log(`Wrote ${output}`);
console.log(`Components: ${components.length}; core dependencies: ${coreComponents.length}; optional plugins: ${pluginComponents.length}; plugin-only libraries: ${uniqueComponents(pluginOnlyLibraryComponents).length}`);
