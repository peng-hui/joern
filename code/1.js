var x = {}
key = "__proto__"
x[key] = { isAdmin: "polluted"};
x.prototype.isAdmin = true;
k = {}
console.log(k.isAdmin)
