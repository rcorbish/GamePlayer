
function Lava( cb ) {
	
	var textureLoader = new THREE.TextureLoader();

	this.uniforms = {
				fogDensity: { value: 0.0013 },
				fogColor:   { value: new THREE.Vector3( 0, 0, 0 ) },
				time:       { value: 1.0 },
				resolution: { value: new THREE.Vector2() },
				uvScale:    { value: new THREE.Vector2( 3.0, 1.0 ) },
				texture1:   { value: textureLoader.load( "textures/cloud.png" ) },
				texture2:   { value: textureLoader.load( "textures/lavatile.jpg" ) }
	};
		
	this.uniforms.texture1.value.wrapS = this.uniforms.texture1.value.wrapT = THREE.RepeatWrapping;
	this.uniforms.texture2.value.wrapS = this.uniforms.texture2.value.wrapT = THREE.RepeatWrapping;
	
	var p1 = loadShader( 'js/lava-vertex.js' ) ;
	var p2 = loadShader( 'js/lava-fragment.js') ; 

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
