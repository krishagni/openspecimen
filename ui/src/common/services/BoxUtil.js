
import numConvUtil from '@/common/services/NumberConverterUtil.js';

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
        matrix[ri][ci].occupied = true;
      }
    } else if (opts.occupants) {
      for (let occupant of opts.occupants) {
        let row = occupant[opts.rowProp];
        let col = occupant[opts.colProp];
        let ri  = assigner.toMapRow({row, col, nr, nc});
        let ci  = assigner.toMapCol({row, col, nr, nc});
        matrix[ri][ci].occupied = occupant;
      }
    }

    return matrix;
  }
}

export default new BoxUtil();
