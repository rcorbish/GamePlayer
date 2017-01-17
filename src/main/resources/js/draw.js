
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

var squareRotation = 0.0;
var squareXOffset = 0.0;
var squareYOffset = 0.0;
var squareZOffset = 0.0;
var lastSquareUpdateTime = 0;
var xIncValue = 0.02;
var yIncValue = -0.04;
var zIncValue = 0.3;


var colors = [
              [0.0,  1.0,  1.0,  1.0],    // Front face: cyan
              [1.0,  0.0,  0.0,  1.0],    // Back face: red
              [0.0,  1.0,  0.0,  1.0],    // Top face: green
              [0.0,  0.0,  1.0,  1.0],    // Bottom face: blue
              [1.0,  1.0,  0.0,  1.0],    // Right face: yellow
              [1.0,  0.0,  1.0,  1.0]     // Left face: purple
              ];


var vertices = [
                // Front face
                -0.5, -2.0,  1.0,
                0.5, -2.0,  1.0,
                0.5,  2.0,  1.0,
                -0.5,  2.0,  1.0,

                // Back face
                -0.5, -2.0, -1.0,
                -0.5,  2.0, -1.0,
                0.5,  2.0, -1.0,
                0.5, -2.0, -1.0,

                // Top face
                -0.5,  2.0, -1.0,
                -0.5,  2.0,  1.0,
                0.5,  2.0,  1.0,
                0.5,  2.0, -1.0,

                // Bottom face
                -0.5, -2.0, -1.0,
                0.5, -2.0, -1.0,
                0.5, -2.0,  1.0,
                -0.5, -2.0,  1.0,

                // Right face
                0.5, -2.0, -1.0,
                0.5,  2.0, -1.0,
                0.5,  2.0,  1.0,
                0.5, -2.0,  1.0,

                // Left face
                -0.5, -2.0, -1.0,
                -0.5, -2.0,  1.0,
                -0.5,  2.0,  1.0,
                -0.5,  2.0, -1.0
                ];

var vertex_indices = [
                      0,  1,  2,      0,  2,  3,    // front
                      4,  5,  6,      4,  6,  7,    // back
                      8,  9,  10,     8,  10, 11,   // top
                      12, 13, 14,     12, 14, 15,   // bottom
                      16, 17, 18,     16, 18, 19,   // right
                      20, 21, 22,     20, 22, 23    // left
                      ] ;

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

//	var generatedColors = [];

//	for (j=0; j<6; j++) {
//	var c = colors[j];

//	// Repeat each color four times for the four vertices of the face

//	for (var i=0; i<4; i++) {
//	generatedColors = generatedColors.concat(c);
//	}
//	}

	var generatedColors = [];

	for (j=0; j<24; j++) {
		generatedColors = generatedColors.concat(colors[j%6]);
	}

	color_buffer = prepareBuffer( shader_program, generatedColors ) ;
	vertex_color_attribute = gl.getAttribLocation(shader_program, "aVertexColor");
	gl.enableVertexAttribArray(vertex_color_attribute);

	vertex_position_buffer = prepareBuffer( shader_program, vertices ) ;
	vertex_position_attribute = gl.getAttribLocation(shader_program, "aVertexPosition");
	gl.enableVertexAttribArray(vertex_position_attribute);

	vertex_index_buffer = gl.createBuffer();
	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(vertex_indices), gl.STATIC_DRAW);

	
//	sphere_vertices = buildSphere() ;
//	sphere_vertex_buffer = prepareBuffer( shader_program, sphere_vertices ) ;
//	sphere_vertex_attribute = gl.getAttribLocation(shader_program, "aSphereVertexPosition");
//	gl.enableVertexAttribArray(sphere_vertex_attribute);
	
	setInterval(drawScene, 10);
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
	gl.bindBuffer(gl.ARRAY_BUFFER, color_buffer );
	gl.vertexAttribPointer(vertex_color_attribute, 4, gl.FLOAT, false, 0, 0); // 4 items per row
//	END COLOR

	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	setMatrixUniforms( shader_program );
	gl.drawElements(gl.TRIANGLES, 36, gl.UNSIGNED_SHORT, 0);

//	SECOND CUBOID
	loadIdentity();
	mvTranslate([-0.0, -2.0, -12.0]);
	mvRotate(squareRotation, [0, 1, 0]);
	mvTranslate([squareXOffset, squareYOffset, squareZOffset]);
//	SET VERTICES	
	gl.bindBuffer(gl.ARRAY_BUFFER, vertex_position_buffer);
	gl.vertexAttribPointer(vertex_position_attribute, 3, gl.FLOAT, false, 0, 0); // 3 items ( ie. 3 x 3 matrix )
//	END VERTEX

//	SET COLORS
	gl.bindBuffer(gl.ARRAY_BUFFER, color_buffer );
	gl.vertexAttribPointer(vertex_color_attribute, 4, gl.FLOAT, false, 0, 0); // 4 items per row
//	END COLOR

	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	setMatrixUniforms( shader_program );

	gl.drawElements(gl.TRIANGLES, 36, gl.UNSIGNED_SHORT, 0);
//	END SECOND CUBOID

//	THIRD CUBOID
	loadIdentity();
	mvTranslate([-0.0, -2.0, -12.0]);
	mvRotate(squareRotation, [0, 0, 1]);
	mvTranslate([squareXOffset, squareYOffset, squareZOffset]);
//	SET VERTICES	
	gl.bindBuffer(gl.ARRAY_BUFFER, vertex_position_buffer);
	gl.vertexAttribPointer(vertex_position_attribute, 3, gl.FLOAT, false, 0, 0); // 3 items ( ie. 3 x 3 matrix )
//	END VERTEX

//	SET COLORS
	gl.bindBuffer(gl.ARRAY_BUFFER, color_buffer );
	gl.vertexAttribPointer(vertex_color_attribute, 4, gl.FLOAT, false, 0, 0); // 4 items per row
//	END COLOR

	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, vertex_index_buffer );
	setMatrixUniforms( shader_program );

	gl.drawElements(gl.TRIANGLES, 36, gl.UNSIGNED_SHORT, 0);
//	END THIRD CUBOID


	
//	SPHERE	
//	gl.bindBuffer(gl.ARRAY_BUFFER, sphere_vertex_buffer);
//	gl.vertexAttribPointer(vertex_position_attribute, 3, gl.FLOAT, false, 0, 0); // 3 items ( ie. 3 x 3 matrix )
//	gl.bindBuffer(gl.ARRAY_BUFFER, color_buffer );
//	gl.vertexAttribPointer(vertex_color_attribute, 4, gl.FLOAT, false, 0, 0); // 4 items per row

//	setMatrixUniforms( shader_program );
//	gl.drawArrays(gl.TRIANGLE_STRIP, 10, gl.UNSIGNED_SHORT, 0);

//	END SPHERE
	

	// Restore non translated & rotated matrix
	mvPopMatrix();

	var currentTime = (new Date).getTime();
	if (lastSquareUpdateTime) {
		var delta = currentTime - lastSquareUpdateTime;

		squareRotation += (30 * delta) / 1000.0;
		squareXOffset += xIncValue * ((30 * delta) / 1000.0);
		squareYOffset += yIncValue * ((30 * delta) / 1000.0);
//		squareZOffset += zIncValue * ((30 * delta) / 1000.0);

		if (Math.abs(squareYOffset) > 2.5) {
			xIncValue = -xIncValue;
			yIncValue = -yIncValue;
			zIncValue = -zIncValue;
		}
	}

	lastSquareUpdateTime = currentTime;

}