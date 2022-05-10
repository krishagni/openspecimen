
import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  format(value, {specimen, metadata, context}) {
    if (value == null || value == undefined) {
      return null;
    }

    let obj = specimen;
    if (!obj) {
      if (!metadata || !metadata.entity || !context) {
        return value;
      }

      obj = exprUtil.eval(context, metadata.entity);
    }

    if (!obj) {
      return value;
    }

    const type = obj.type || obj.specimenType;
    if (!obj.specimenClass && !type) {
      return value;
    }

    const measure = (metadata && metadata.measure) || 'quantity';
    const unit = util.getSpecimenMeasureUnit(obj, measure);
    return unit && (value + ' ' + unit) || value;
  }
}
