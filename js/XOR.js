// Maybe should call it Swizzler
function XOR()
{
}

XOR.OBJECTREF = '|XOR|';
XOR.ID = 'XOR.id';
 
// Swizzle object references with a string value, which is a synthetic id 
// If an object is encountered for the first time, its reference is not swizzled
XOR.pack = function(jsonObj)
{
	var unprocessed = [];
	var marked = new Set();
	var currentID = 10000;
	
	unprocessed.push(jsonObj);
			
	if(jsonObj.hasOwnProperty(XOR.ID)) {
	  console.log('JSON instance has already been packed');
	  return;
	}
	
	while(unprocessed.length > 0) {
		var element = unprocessed.pop();
		if(marked.has(element)) {
		  continue;
		}
		
		if(element != null && Array.isArray(element)) {
		    for (var i = 0; i < element.length; i++) {
			    var child = element[i];
				if(child != null && Array.isArray(child)) {
				    unprocessed.push(obj);
				} else if(child != null && typeof child === 'object'){
				    if(!child.hasOwnProperty(ID)) {
					    unprocessed.push(child);
					}
				}
			}
			marked.add(element);
		} else if(element !== null && typeof element === 'object') {
		   element[XOR.ID] = currentID++;
		   
		   for (var p in element) {
		      var child = element[p];
			  if(child != null && Array.isArray(child)) {
				unprocessed.push(child);
			  } else if(child !== null && typeof child === 'object') {
			      if(!child.hasOwnProperty(XOR.ID)) {
				      unprocessed.push(child);
				  } else {
				      var id = child[XOR.ID];
					  element[XOR.OBJECTREF+p] = id;
					  delete element[p];
				  }
			  } 
           }
		   marked.add(element);
		} 
	}
}

// Swizzle the id of type string with the actual object reference
XOR.unpack = function(jsonObj)
{
	var idMap = {};
	var unprocessed = [];
	unprocessed.push(jsonObj);	
	
	if(!jsonObj.hasOwnProperty(XOR.ID)) {
	  console.log('JSON instance has not been packed');
	  return;
	}	
	
	// populate the idMap
	while(unprocessed.length > 0) {
		var element = unprocessed.pop();
		
		if(element != null && Array.isArray(element)) {		
		    for (var i = 0; i < element.length; i++) {
			    var child = element[i];
				if(child != null && typeof child === 'object' ) {
				    unprocessed.push(obj);
				} 
			}	
		} else if(element !== null && typeof element === 'object') {
			if(!element.hasOwnProperty(XOR.ID)) {
				throw 'XOR Id not found, object has not be fully packed';
			}	
			if(idMap.hasOwnProperty(element[XOR.ID])) {
				throw '"Multiple objects found with same id, or object was modified after being packed';
			}
			idMap[element[XOR.ID]] = element;
			delete element[XOR.ID];
			
			for (var p in element) {
				if(element[p] != null && typeof element[p] === 'object' ) {
					unprocessed.push(element[p]);
				}
			}
		}
	}
	
	// replace object references 
	unprocessed = [];
	var marked = new Set();	
	
	unprocessed.push(jsonObj);
	while(unprocessed.length > 0) {
		var element = unprocessed.pop();
		if(marked.has(element)) {
		  continue;
		}
		
		if(element != null && Array.isArray(element)) {
		    for (var i = 0; i < element.length; i++) {
			    var child = element[i];
				if(child != null && typeof child === 'object'){
				    unprocessed.push(child);
				}
			}
			marked.add(element);
		} else if(element !== null && typeof element === 'object') {
			for (var p in element) {
				if(p.lastIndexOf(XOR.OBJECTREF, 0) === 0) {
					var objRef = idMap[element[p]];
					var newKey = p.substring(XOR.OBJECTREF.length);
					element[newKey] = objRef;
					delete element[p];
				} else {
					if(element[p] !== null && typeof element[p] === 'object') {
						unprocessed.push(element[p]);
					}
				}
			}
			marked.add(element);
		}
	}
}

var obj = {"displayName":"Dilip Dalton","name":"DILIP_DALTON","description":"Software engineer in the bay area","userName":"daltond"};
obj["self"] = obj;
var another = {"another":"Tell me"};
obj["second"] = another;
for (var p in obj) {
    console.log(p+': '+obj[p]);
}
console.log("****** PACK ********");
XOR.pack(obj);
for (var p in obj) {
    console.log(p+': '+obj[p]);
}

console.log("****** UNPACK ******");
XOR.unpack(obj)
for (var p in obj) {
    console.log(p+': '+obj[p]);
}