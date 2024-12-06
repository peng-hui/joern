module.exports.apply = apply;
function apply(changes, target, modify) {
  var obj, clone;
  if (modify) {
    obj = target;
  } else {
    clone = require("udc");
    obj = clone(target);
  }
  changes.forEach(function (ch) {
    var ptr, keys, len;
    switch (ch.type) {
      case 'put':
        ptr = obj;
        keys = ch.key;
        len = keys.length;
        if (len) {
          keys.forEach(function (prop, i) {
            if (!(prop in ptr)) {
              ptr[prop] = {};
            }

            if (i < len - 1) {
              ptr = ptr[prop];
            } else {
              ptr[prop] = ch.value;
            }
          });
        } else {
          obj = ch.value;
        }
        break;

      case 'del':
        ptr = obj;
        keys = ch.key;
        len = keys.length;
        if (len) {
          keys.forEach(function (prop, i) {
            if (!(prop in ptr)) {
              ptr[prop] = {};
            }

            if (i < len - 1) {
              ptr = ptr[prop];
            } else {
              delete ptr[prop];
            }
          });
        } else {
          obj = null;
        }
        break;
    }
  });
  return obj;
}
