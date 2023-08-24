
import numConvUtil from '@/common/services/NumberConverterUtil.js';
import util from '@/common/services/Util.js';

class BoxUtil {

  positionAssigners = {
    'HZ_TOP_DOWN_LEFT_RIGHT': new function() {
      this.row = function({ri}) { return ri + 1; }

      this.col = function({ci}) { return ci + 1; }

      this.pos = function({row, col, nc}) { return (row - 1) * nc + col }

      this.fromPos = function({pos, nc}) { return {row: Math.floor((pos - 1) / nc) + 1, column: (pos - 1) % nc + 1}; }

      this.toMapRow = function({row}) { return row - 1; }

      this.toMapCol = function({col}) { return col - 1; }

      this.rowMajor = function() { return true; }
    },

    'HZ_TOP_DOWN_RIGHT_LEFT': new function() {
      this.row = function({ri}) { return ri + 1; }

      this.col = function({ci, nc}) { return nc - ci; }

      this.pos = function({row, col, nc}) { return (row - 1) * nc + col }

      this.fromPos = function({pos, nc}) { return {row: Math.floor((pos - 1) / nc) + 1, column: (pos - 1) % nc + 1}; }

      this.toMapRow = function({row}) { return row - 1; }

      this.toMapCol = function({col, nc}) { return nc - col; }

      this.rowMajor = function() { return true; }
    },

    'HZ_BOTTOM_UP_LEFT_RIGHT': new function() {
      this.row = function({ri, nr}) { return nr - ri; }

      this.col = function({ci}) { return ci + 1; }

      this.pos = function({row, col, nc}) { return (row - 1) * nc + col }

      this.fromPos = function({pos, nc}) { return {row: Math.floor((pos - 1) / nc) + 1, column: (pos - 1) % nc + 1}; }

      this.toMapRow = function({row, nr}) { return nr - row; }

      this.toMapCol = function({col}) { return col - 1; }

      this.rowMajor = function() { return true; }
    },

    'HZ_BOTTOM_UP_RIGHT_LEFT': new function() {
      this.row = function({ri, nr}) { return nr - ri; }

      this.col = function({ci, nc}) { return nc - ci; }

      this.pos = function({row, col, nc}) { return (row - 1) * nc + col }

      this.fromPos = function({pos, nc}) { return {row: Math.floor((pos - 1) / nc) + 1, column: (pos - 1) % nc + 1}; }

      this.toMapRow = function({row, nr}) { return nr - row; }

      this.toMapCol = function({col, nc}) { return nc - col; }

      this.rowMajor = function() { return true; }
    },

    'VT_TOP_DOWN_LEFT_RIGHT': new function() {
      this.row = function({ci}) { return ci + 1; }

      this.col = function({ri}) { return ri + 1; }

      this.pos = function({row, col, nr}) { return (row - 1) * nr + col }

      this.fromPos = function({pos, nr}) { return {row: Math.floor((pos - 1) / nr) + 1, column: (pos - 1) % nr + 1}; }

      this.toMapRow = function({col}) { return col - 1; }

      this.toMapCol = function({row}) { return row - 1; }

      this.rowMajor = function() { return false; }
    },

    'VT_TOP_DOWN_RIGHT_LEFT': new function() {
      this.row = function({ci, nc}) { return nc - ci; }

      this.col = function({ri}) { return ri + 1; }

      this.pos = function({row, col, nr}) { return (row - 1) * nr + col }

      this.fromPos = function({pos, nr}) { return {row: Math.floor((pos - 1) / nr) + 1, column: (pos - 1) % nr + 1}; }

      this.toMapRow = function({col}) { return col - 1; }

      this.toMapCol = function({row, nc}) { return nc - row; }

      this.rowMajor = function() { return false; }
    },

    'VT_BOTTOM_UP_LEFT_RIGHT': new function() {
      this.row = function({ci}) { return ci + 1; }

      this.col = function({ri, nr}) { return nr - ri; }

      this.pos = function({row, col, nr}) { return (row - 1) * nr + col }

      this.fromPos = function({pos, nr}) { return {row: Math.floor((pos - 1) / nr) + 1, column: (pos - 1) % nr + 1}; }

      this.toMapRow = function({col, nr}) { return nr - col; }

      this.toMapCol = function({row}) { return row - 1; }

      this.rowMajor = function() { return false; }
    },

    'VT_BOTTOM_UP_RIGHT_LEFT': new function() {
      this.row = function({ci, nc}) { return nc - ci; }

      this.col = function({ri, nr}) { return nr - ri; }

      this.pos = function({row, col, nr}) { return (row - 1) * nr + col }

      this.fromPos = function({pos, nr}) { return {row: Math.floor((pos - 1) / nr) + 1, column: (pos - 1) % nr + 1}; }

      this.toMapRow = function({col, nr}) { return nr - col; }

      this.toMapCol = function({row, nc}) { return nc - row; }

      this.rowMajor = function() { return false; }
    }
  }

  getPositionAssigner(type) {
    return this.positionAssigners[type];
  }

  //
  // opts: {
  //   positionAssignerType,
  //   rowLabelingScheme,
  //   columnLabelingScheme,
  //   numberOfRows,
  //   numberOfColumns,
  //   occupiedPositions - linear positions,
  //   occupants
  // }
  //
  getMatrix(opts) {
    const assignerType = opts.positionAssignerType || 'HZ_TOP_DOWN_LEFT_RIGHT';
    const assigner     = this.getPositionAssigner(assignerType);

    const nr = opts.numberOfRows;
    const nc = opts.numberOfColumns;

    let matrix = new Array(nr);
    for (let ri = 0; ri < nr; ++ri) {
      matrix[ri] = new Array(nc);

      for (let ci = 0; ci < nc; ++ci) {
        let row = assigner.row({ri, ci, nr, nc});
        let col = assigner.col({ri, ci, nr, nc});
        matrix[ri][ci] = {
          row:       row,
          column:    col,
          rowStr:    numConvUtil.fromNumber(opts.rowLabelingScheme, row),
          columnStr: numConvUtil.fromNumber(opts.columnLabelingScheme, col),
          position:  assigner.pos({row, col, nr, nc})
        };
      }
    }

    if (opts.occupiedPositions) {
      for (let occupant of opts.occupiedPositions) {
        let {row, column} = assigner.fromPos({pos: occupant, nr, nc});
        let ri = assigner.toMapRow({row, col: column, nr, nc});
        let ci = assigner.toMapCol({row, col: column, nr, nc});
        if (ri < matrix.length && ci < matrix[ri].length) {
          matrix[ri][ci].occupied = true;
        }
      }
    } else if (opts.occupants) {
      for (let occupant of opts.occupants) {
        let row = occupant[opts.rowProp];
        let col = occupant[opts.colProp];
        let ri  = assigner.toMapRow({row, col, nr, nc});
        let ci  = assigner.toMapCol({row, col, nr, nc});
        if (ri < matrix.length && ci < matrix[ri].length) {
          matrix[ri][ci].occupied = occupant;
        }
      }
    }

    return matrix;
  }

  //
  // opts: {
  //   positionAssignerType,
  //   positionLabelingMode
  //   rowLabelingScheme,
  //   columnLabelingScheme,
  //   numberOfRows,
  //   numberOfColumns,
  //   occupiedPositions - linear positions,
  //   occupants
  //   isVacatable: input <- occupant, output -> true/falseboolean fn
  //   occupantName: input <- occupant, output -> occupant name
  //   createCell: (label, row, column, existing) => cell
  // }
  //
  assignPositions(opts, inputLabels, vacateOccupants) {
    const assignerType = opts.positionAssignerType || 'HZ_TOP_DOWN_LEFT_RIGHT';
    const assigner     = this.getPositionAssigner(assignerType);
    const occupants    = opts.occupants || [];

    //  
    // Below regular expression will break (A, B) x, y, z into following parts
    // match[0] = '"(A, B)" x, y, z' ; entire matched string
    // match[1] = '"(A, B)"'; starting cell in parenthesis
    // match[2] = '(A, B)'; starting cell without parenthesis
    // match[3] = 'x, y, z'; labels to scan or parse
    //  
    const re = /("([^"]+)")?\s*([^"]+)/g;
    inputLabels = (inputLabels && inputLabels.trim()) || ''; 

    const input = [];
    let match = null;
    while ((match = re.exec(inputLabels)) != null) {
      let startCell = match[2] && match[2].trim();
      if (startCell && startCell.charAt(0) == '(' && startCell.charAt(startCell.length - 1) == ')') {
        startCell = startCell.substr(1, startCell.length - 2); 
      }   

      input.push({startCell: startCell, labels: util.splitStr(match[3], /,|\t|\n/, true)});
    }

    let noFreeLocs = false
    for (let i = 0; i < input.length; ++i) {
      let startRow = 1, startColumn = 1;
      let mapIdx = 0, labelIdx = 0;

      let labels = input[i].labels;
      if (input[i].startCell) {
        //
        // Explicit starting cell specified
        //
        let startCell = input[i].startCell.trim().split(',');
        if (opts.positionLabelingMode == 'LINEAR') {
          let {row, column} = assigner.fromPos({pos: +startCell, nr: opts.numberOfRows, nc: opts.numberOfColumns});
          startRow    = row
          startColumn = column;
        } else {
          if (startCell.length != 2) {
            alert("Invalid start position: " + input[i].startCell);
            return;
          }

          startRow    = numConvUtil.toNumber(opts.rowLabelingScheme,    startCell[0].trim());
          startColumn = numConvUtil.toNumber(opts.columnLabelingScheme, startCell[1].trim());
        }

        //
        // fast forward map index to point to first occupant in cell (row, column)
        // such that row > startRow or row == startRow and column > startColumn
        //
        while (mapIdx < opts.occupants.length) {
          let row    = opts.occupants[mapIdx][opts.rowProp];
          let column = opts.occupants[mapIdx][opts.colProp];
          if (row < startRow || (row == startRow && column < startColumn)) {
            ++mapIdx;
          } else {
            break;
          }

          ++mapIdx;
        }
      }

      const yLimit = assigner.rowMajor() ? opts.numberOfRows : opts.numberOfColumns;
      const xLimit = assigner.rowMajor() ? opts.numberOfColumns : opts.numberOfRows;
      let done = false;
      for (let y = startRow; y <= yLimit; ++y) {
        for (let x = startColumn; x <= xLimit; ++x) {
          if (labelIdx >= labels.length) {
            //  
            // we are done with probing/iterating through all input labels
            //  
            done = true;
            break;
          }   

          let existing = undefined;
          if (mapIdx < occupants.length && occupants[mapIdx][opts.rowProp] == y && occupants[mapIdx][opts.colProp] == x) {
            //  
            // current cell is occupied
            //  
            if (!vacateOccupants || !opts.isVacatable(occupants[mapIdx])) {
              //  
              // When asked not to vacate existing occupants or present occupant
              // is not vacatable, then examine next cell
              //  
              mapIdx++;
              continue;
            }   

            existing = occupants[mapIdx];
            occupants.splice(mapIdx, 1); 
          }   
 
          var label = labels[labelIdx++];
          if ((!label || label.trim().length == 0) && (!vacateOccupants || !existing)) {
            //
            // Label is empty. Either asked not to vacate existing occupants or
            // present cell is empty
            //
            continue;
          }

          let cell = undefined;
          if (!!existing && opts.occupantName(existing).toLowerCase() == label.toLowerCase()) {
            cell = existing;
          } else {
            cell = opts.createCell(label, y, x, existing);
          }

          occupants.splice(mapIdx, 0, cell);
          mapIdx++;
        }

        //
        // start of next row
        //
        startColumn = 1;

        if (done) {
          break;
        }
      }

      while (labelIdx < labels.length) {
        if (!!labels[labelIdx] && labels[labelIdx].trim().length > 0) {
          noFreeLocs = true;
          break;
        }

        labelIdx++;
      }
    }

    return {occupants: occupants, noFreeLocs: noFreeLocs};
  }
}

export default new BoxUtil();
