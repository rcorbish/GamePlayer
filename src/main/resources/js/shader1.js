
function Shader1( cb ) {
	
	var textureLoader = new THREE.TextureLoader();

	this.uniforms = {
			time:       { value: 1.0 },
			resolution: { value: new THREE.Vector2() }
		};
		
	var p1 = loadShader( 'js/shader1-vertex.js' ) ;
	var p2 = loadShader( 'js/shader1-fragment.js') ; 

	Promise.all( [p1, p2 ] )
	.then( function( data ) {
		this.material = new THREE.ShaderMaterial( { 
			uniforms: this.uniforms,
			vertexShader: data[0],
			fragmentShader: data[1]
		} ) ;
		if( cb ) cb() ;
	}.bind( this ) ) 
	.catch( function(err) {
		console.log( err ) ;
	})
	
	function loadShader( url, callback ) {   
		return new Promise( function( resolve, reject ) {
			var xobj = new XMLHttpRequest();
			xobj.open('GET', url, true); 
			xobj.onreadystatechange = function () {
	          if (xobj.readyState == 4 && xobj.status === 200 ) {
	            resolve(xobj.responseText);
	          }
			};
			xobj.send(null);
		} ) ;
	} ;
	
}
