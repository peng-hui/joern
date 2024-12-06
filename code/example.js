function apply(arg1, arg2) { /* ... */ }
module.exports = apply;
module.exports.func = function(arg) { /* ... */ };
module.exports = {diffApply, a};
function diffApply(obj, diff){
    var lastProp = diff.path.pop();
    var thisProp;
    while ((thisProp = diff.path.shift()) != null){
        if (!(thisProp in obj)) {
            obj[thisProp]= {};
        }
            obj = obj[thisProp];
    }
    if (diff.op === REPLACE || diff.op === ADD){
        obj[lastProp]= diff.value;
    }
}
function MyFunction(arg1, arg2) { /* ... */ }
MyFunction.prototype.method = function(arg) { /* ... */ };
exports["dynamicProp"] = function(dynamicArg) { /* ... */ };
module.exports = { MyFunction, anotherFunc };

