
var hilbert_map = {
    'a': [ {quad_position:0, square:'d'}, {quad_position:1, square:'a'}, {quad_position:3, square:'b'}, {quad_position:2, square:'a'} ],
    'b': [ {quad_position:2, square:'b'}, {quad_position:1, square:'b'}, {quad_position:3, square:'a'}, {quad_position:0, square:'c'} ],
    'c': [ {quad_position:2, square:'c'}, {quad_position:3, square:'d'}, {quad_position:1, square:'c'}, {quad_position:0, square:'b'} ],
    'd': [ {quad_position:0, square:'a'}, {quad_position:3, square:'c'}, {quad_position:1, square:'d'}, {quad_position:2, square:'d'} ]
} ;

function point_to_hilbert(x, y, order) {
	var current_square = 'a' ;
	var position = 0 ;

	for ( var i=order-1 ; i>=0 ; i-- ) {
		var mask = 1 << i ;
		position = position * 4 ;
		var quad_x = ( x & mask ) ? 1 : 0 ;
		var quad_y = ( y & mask ) ? 1 : 0 ;
		var ix = (quad_x << 1) + quad_y ;
		var r = hilbert_map[current_square][ix] ;
		current_square = r.square ;
		position += r.quad_position
	}
	return position
}


function hilbertMap(quadits) {
	if ( quadits.length === 0 ) {
		return { x:0, y:0 };   // center of the square
	}
	return (function() {
		var t = quadits[0];          		// get first quadit
		//	console.log( t );
		var pt = hilbertMap(quadits.slice(1));  // recursive call
		console.log( pt ) ;
		var x = pt.x;
		var y = pt.y;
		switch(t) {
		case "0":     // southwest, with a twist
			return { x: y/2, 		y: x/2 } ;
		case "1":     // northwest
			return { x: x/2, 		y: (y+1)/2 } ;
		case "2":     // northeast
			return { x: (x+1)/2, 	y: (y+1)/2 } ;
		case "3":     // southeast, with twist & flip
			return { x: 1-y/2, 	y: (1-x)/2 } ;
		}
	})() ;
}

module.exports.point_to_hilbert = point_to_hilbert ;
/*
var order = 8 ;
var p = point_to_hilbert(3, 1, order) ;
var n = Math.pow( 4, order ) ;
console.log( "Hilbert ix: ", p, "of", n, "=", Number( p/n ).toString(4), "(4)" ) ;
return ;
var qb = "21003331222202020112203" ;
console.log( hilbertMap( qb ) ) ;
var denom = Math.pow( 4, qb.length ) ;
var num = parseInt( qb, 4 ) ;
console.log( "t= (", num, "/", denom, " ) ", num / denom, "(10) ", Number(num/denom).toString(4), "(4)" ) ;
*/