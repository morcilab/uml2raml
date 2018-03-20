var gulp = require('gulp');
var path = require('path');

var CWD = path.resolve('.');
var API_SPEC = path.resolve(CWD, '${raml.file}');
var API_HTML = path.resolve(CWD, '${api.docfile}');

gulp.task('default', function() {
	console.log("API_SPEC: "+API_SPEC);
	console.log("API_HTML: "+API_HTML);
	const raml2html = require('raml2html');
	var fs = require('fs');
	const configWithDefaultTheme = raml2html.getConfigForTheme();
	raml2html.render(API_SPEC, configWithDefaultTheme).then(
		function(result) {
			fs.writeFile(API_HTML, result, 
				(err) => {
					if(err) {
						console.error(err.toString());
						this.emit('end');
					}
				}
			);
		}, 
		function(error) {
			console.error(error.toString());
			this.emit('end');
		});
});
