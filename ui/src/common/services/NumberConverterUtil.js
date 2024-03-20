
class NumberConverterUtil {

  fromNumber(scheme, number) {
    switch(scheme) {
      case 'Numbers':
        return this._toNum(number);
      case 'Alphabets Upper Case':
        return this._toUpperAlphabet(number);
      case 'Alphabets Lower Case':
        return this._toLowerAlphabet(number);
      case 'Roman Upper Case':
        return this._toUpperRoman(number);
      case 'Roman Lower Case':
        return this._toLowerRoman(number);
      default:
        return 'XXX: ' + number;
    }
  }

  toNumber(scheme, str) {
    str = '' + str;

    switch(scheme) {
      case 'Numbers':
        return this._fromNum(str);
      case 'Alphabets Upper Case':
        return this._fromUpperAlphabet(str);
      case 'Alphabets Lower Case':
        return this._fromLowerAlphabet(str);
      case 'Roman Upper Case':
        return this._fromUpperRoman(str);
      case 'Roman Lower Case':
        return this._fromLowerRoman(str);
      default:
        return 'YYY: ' + str;
    }
  }

  _toAlphabet(num, aCharCode) {
    let result = "";
    while (num > 0) {
      result = String.fromCharCode((num - 1) % 26 + aCharCode) + result;
      num = Math.floor((num - 1) / 26);
    }

    return result;
  }

  _fromAlphabet(str, aCharCode) {
    let len = str.length;
    let base = 1, result = 0;

    while (len > 0) {
      len--;

      result += (str.charCodeAt(len) - aCharCode + 1) * base;
      base *= 26;
    }

    return result;
  }

  _romanLookupTab = {
    M: 1000, CM: 900,
    D:  500, CD: 400,
    C:  100, XC:  90,
    L:   50, XL:  40,
    X:   10, IX:   9,
    V:    5, IV:   4,
    I:    1
  };

  _toRoman(num, upper) {
    let result = '';
    for (var i in this._romanLookupTab) {
      while (num >= this._romanLookupTab[i]) {
        result += i;
        num -= this._romanLookupTab[i];
      }
    }
 
    return upper ? result : result.toLowerCase();
  }

  _fromRoman(str) {
    str = str.toUpperCase();

    let result = 0;
    let len = str.length, idx = str.length;
    while (idx > 0) {
      --idx;
      if (idx == len - 1) {
        let val = this._romanLookupTab[str.substring(idx, idx + 1)];
        if (!val) {
          alert("Invalid roman number: " + str);
        }

        result += val;
      } else {
        let current = this._romanLookupTab[str.substring(idx, idx + 1)];
        let ahead   = this._romanLookupTab[str.substring(idx + 1, idx + 2)];
        if (!current || !ahead) {
          alert("Invalid roman number: " + str);
        }

        if (current < ahead) {
          result -= current;
        } else {
          result += current;
        }
      }
    }

    return result;
  }

  _toNum(num) {
    return num;
  }

  _fromNum(str) {
    return +str;
  }

  _toUpperAlphabet(num) {
    return this._toAlphabet(num, 'A'.charCodeAt(0));
  }

  _fromUpperAlphabet(str) {
    return this._fromAlphabet(str, 'A'.charCodeAt(0));
  }

  _toLowerAlphabet(num) {
    return this._toAlphabet(num, 'a'.charCodeAt(0));
  }

  _fromLowerAlphabet(str) {
    return this._fromAlphabet(str, 'a'.charCodeAt(0));
  }

  _toUpperRoman(num) {
    return this._toRoman(num, true);
  }

  _fromUpperRoman(str) {
    return this._fromRoman(str, true);
  }

  _toLowerRoman(num) {
    return this._toRoman(num, false);
  }

  _fromLowerRoman(str) {
    return this._fromRoman(str, false);
  }
}

export default new NumberConverterUtil();
