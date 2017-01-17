
var gl; // A global variable for the WebGL context
var shader_program ;
var color_buffer ;
var vertex_color_attribute ;
var vertex_position_buffer ;
var vertex_position_attribute ;
var vertex_index_buffer ;
var vertex_index_attribute ;
var sphere_vertices ;
var sphere_vertex_buffer ;
var sphere_vertex_attribute ;
var num_vertices ;

var squareRotation = 0.0;
var squareXOffset = 0.0;
var squareYOffset = 0.0;
var squareZOffset = 0.0;
var lastSquareUpdateTime = 0;
var xIncValue = 0.02;
var yIncValue = -0.04;
var zIncValue = 0.003;


var colors = [
              [0.0,  1.0,  1.0,  1.0],    // Front face: cyan
              [1.0,  0.0,  0.0,  1.0],    // Back face: red
              [0.0,  0.3,  0.04,  1.0],    // Top face: green
              [0.0,  0.0,  1.0,  1.0],    // Bottom face: blue
              [1.0,  1.0,  0.0,  1.0],    // Right face: yellow
              [1.0,  0.0,  1.0,  1.0]     // Left face: purple
              ];

function start() {
	var canvas = document.getElementById("glcanvas");

	// Initialize the GL context
	gl = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");

	// Only continue if WebGL is available and working

	if (gl) {
		gl.viewport(0, 0, canvas.width, canvas.height);
		// Set clear color to black, fully opaque
		gl.clearColor(0.0, .1, 0.05, 1.0);
		// Enable depth testing
		gl.enable(gl.DEPTH_TEST);
		// Near things obscure far things
		gl.depthFunc(gl.LEQUAL);
		// Clear the color as well as the depth buffer.
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
	}  


	shader_program = get_program() ;
	buildSphere() ;

	var generatedColors = [];
	for (j=0; j<num_vertices; j++) {
		generatedColors = generatedColors.concat(colors[(j>>2)%2]);
	}

	color_buffer = prepareBuffer( shader_program, generatedColors ) ;

	vertex_color_attribute = gl.getAttribLocation(shader_program, "aVertexColor");
	gl.enableVertexAttribArray(vertex_color_attribute);
	
	setInterval(drawScene, 2);
}

function get_program() {
	// Load the shaders 
	var vertex_shader = getVertexShader() ;
	var fragment_shader = getFragmentShader() ;

	// then link them into a Open GL program
	var shader_program = gl.createProgram();
	gl.attachShader(shader_program, vertex_shader);
	gl.attachShader(shader_program, fragment_shader);
	gl.linkProgram(shader_program);

	// Errors linking the two together?
	if (!gl.getProgramParameter(shader_program, gl.LINK_STATUS)) {
		throw "Unable to initialize the shader program." ;
	}

	gl.useProgram(shader_program);

	return shader_program ;
}

function getFragmentShader() {
	return getShader( 'x-fragment') ;
}

function getVertexShader() {
	return getShader( 'x-vertex') ;
}


function getShader( shader_type ) {
	// Load the source
	var shaderSrc = $("script[type='x-shader/" + shader_type + "']").html() ;
	// Create a shader
	var shader = gl.createShader( shader_type=='x-vertex' ? gl.VERTEX_SHADER : gl.FRAGMENT_SHADER);
	// attach the source
	gl.shaderSource(shader, shaderSrc);

	// Compile the shader program
	gl.compileShader(shader);  

	// See if it compiled successfully
	if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {  
		throw ("An error occurred compiling the shaders: " + gl.getShaderInfoLog(shader));  
	}

	return shader;
}

var latitudeBands = 17;
var longitudeBands = 21;
var radius = 3;
var vertexPositionBuffer;
var vertexIndexBuffer;

function buildSphere() {

	var vertexPositionData = [];
	for (var latNumber = 0; latNumber <= latitudeBands; latNumber++) {
		var theta = latNumber * Math.PI / latitudeBands;
		var sinTheta = Math.sin(theta);
		var cosTheta = Math.cos(theta);
		for (var longNumber = 0; longNumber <= longitudeBands; longNumber++) {
			var phi = longNumber * 2 * Math.PI / longitudeBands;
			var sinPhi = Math.sin(phi);
			var cosPhi = Math.cos(phi);
			var x = cosPhi * sinTheta;
			var y = cosTheta;
			var z = sinPhi * sinTheta;
			vertexPositionData.push(radius * x);
			vertexPositionData.push(radius * y);
			vertexPositionData.push(radius * z);
		}
	}

	var indexData = [];
	for (var latNumber = 0; latNumber < latitudeBands; latNumber++) {
		for (var longNumber = 0; longNumber < longitudeBands; longNumber++) {
			var first = (latNumber * (longitudeBands + 1)) + longNumber;
			var second = first + longitudeBands + 1;
			indexData.push(first);
			indexData.push(second);
			indexData.push(first + 1);
			indexData.push(second);
			indexData.push(second + 1);
			indexData.push(first + 1);
		}
	}
	
	vertex_position_buffer = prepareBuffer( shader_program, vertexPositionData ) ;
	vertex_position_attribute = gl.getAttribLocation(shader_program, "aVertexPosition");
	gl.enableVertexAttribArray(vertex_position_attribute);

	vertex_index_buffer = gl.createBuffer();
	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(indexData), gl.STATIC_DRAW);	
	num_vertices = indexData.length ;
	
}


/**
 * Binds a local data buffer to an OpenGL buffer in the GPU space
 * example:
 * var vertex_buf = prepareBuffer( shader_prg, [ 1,2,3,4...] ) ; 
 * 
 * @param shader_program 
 * @param data
 * @returns a buffer that can be rebound to the GL context via an attribute
 */
function prepareBuffer( shader_program, data ) {

	var buffer = gl.createBuffer();
	gl.bindBuffer(gl.ARRAY_BUFFER, buffer );
	gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.STATIC_DRAW);

	return buffer ;
}

var divisor = 2 ; 
function drawScene() {
	gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

	perspectiveMatrix = makePerspective(45, 1.0, 0.1, 100.0);

	// Set drawing perspective 
	loadIdentity();
	mvTranslate([-0.0, 0.0, -12.0]);

	mvPushMatrix();
	mvRotate(squareRotation, [1, 0, 0]);
	mvTranslate([squareXOffset, squareYOffset, squareZOffset]);

//	SET VERTICES	
	gl.bindBuffer(gl.ARRAY_BUFFER, vertex_position_buffer);
	gl.vertexAttribPointer(vertex_position_attribute, 3, gl.FLOAT, false, 0, 0); // 3 items ( ie. 3 x 3 matrix )
//	END VERTEX

//	SET COLORS
	var generatedColors = [];
	for (j=0; j<num_vertices; j++) {
		if( j >= divisor ) {
			generatedColors = generatedColors.concat(colors[1]);
			generatedColors = generatedColors.concat(colors[2]);
			generatedColors = generatedColors.concat(colors[3]);
			j+=2 ;
		} else {
			generatedColors = generatedColors.concat(colors[0]);
		}
	}

	gl.bindBuffer(gl.ARRAY_BUFFER, color_buffer );
	gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(generatedColors), gl.STATIC_DRAW);
	gl.vertexAttribPointer(vertex_color_attribute, 4, gl.FLOAT, false, 0, 0); // 4 items per row
//	END COLOR

	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	setMatrixUniforms( shader_program );
	gl.drawElements(gl.TRIANGLES, num_vertices, gl.UNSIGNED_SHORT, 0);


	// Restore non translated & rotated matrix
	mvPopMatrix();

	var currentTime = (new Date).getTime();
	if (lastSquareUpdateTime) {
		var delta = currentTime - lastSquareUpdateTime;
		divisor ++ ;
		if( divisor > num_vertices ) { divisor = 0 ; }
		squareRotation += (30 * delta) / 500.0;
		squareXOffset += xIncValue * ((30 * delta) / 500.0);
		squareYOffset += yIncValue * ((30 * delta) / 700.0);
		squareZOffset += zIncValue * ((30 * delta) / 1000.0);

		if (Math.abs(squareYOffset) > 2.5) {
			xIncValue = -xIncValue;
			yIncValue = -yIncValue;
			zIncValue = -zIncValue;
		}
	}

	lastSquareUpdateTime = currentTime;

}