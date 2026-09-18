class HtmlLinkPolicy {
  transformAnchor(attribs, allowedOrigins, applicationBaseUrl) {
    const href = this._getAllowedHref(attribs.href, allowedOrigins, applicationBaseUrl);
    if (!href) {
      return {tagName: 'span', attribs: {style: attribs.style}};
    }

    const result = {href, style: attribs.style};
    if (attribs.target === '_blank') {
      result.target = '_blank';
      result.rel = 'noopener noreferrer';
    } else {
      result.target = '_self';
    }

    return {tagName: 'a', attribs: result};
  }

  _getAllowedHref(href, allowedOrigins, applicationBaseUrl) {
    if (typeof href !== 'string' || !href.trim()) {
      return null;
    }

    // Reject controls and backslashes before the URL parser can normalize them.
    const hasControls = Array.from(href).some(char => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127);
    if (hasControls || href.includes('\\') || href.trim().startsWith('//')) {
      return null;
    }

    try {
      const base = new URL(applicationBaseUrl);
      const url = new URL(href.trim(), base);
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
        return null;
      }

      const origins = Array.isArray(allowedOrigins) ? allowedOrigins : [];
      return url.origin === base.origin || origins.some(origin => new URL(origin).origin === url.origin) ? url.href : null;
    } catch (e) {
      return null;
    }
  }
}

export default new HtmlLinkPolicy();
