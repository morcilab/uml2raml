var gulp = require('gulp');
var path = require('path');

var CWD = path.resolve('.');
var RAML_FILE = path.resolve(CWD, '${raml.file}');
var OAS_FILE = path.resolve(CWD, '${oas.file}');

Array.includes = function() {
    let [first, rest] = arguments;
    return Array.prototype.includes.apply(first, rest);
}

gulp.task('default', function() {
	console.log("RAML_FILE: "+RAML_FILE);
	console.log("OAS_FILE: "+OAS_FILE);
	var converter = require('oas-raml-converter');
	var raml10ToOas30 = new converter.Converter(converter.Formats.RAML, converter.Formats.OAS30);
	raml10ToOas30.convertFile(RAML_FILE).then(function(oas) {
		var fs = require('fs');
		fs.writeFile(OAS_FILE, JSON.stringify(oas, undefined, 2), 
		(err) => {
			if(err) {
				console.error(err.toString());
			}
		});
	}).catch(function(err) {
		console.error(err);
	});	
});
