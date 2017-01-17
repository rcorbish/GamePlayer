/**
 * http://usejsdoc.org/
 */

var hilbert = require( './ifscurve.js' ) ;
var fs = require( "fs" ) ;
var algos = require( "algos" ) ;

function readLines(input, func) {
	var remaining = '';

	input.on('data', function(data) {
		remaining += data;
		var index = remaining.indexOf('\n');
		while (index > -1) {
			var line = remaining.substring(0, index);
			remaining = remaining.substring(index + 1);
			func(line.trim());
			index = remaining.indexOf('\n');
		}
	});

	input.on('end', function() {
		if (remaining.length > 0) {
			func(remaining.trim());
		}
		process() ;
	});
}
var in_coords = false ;
var coords = [] ;
function func(data) {
	if( data==="EOF") {
		in_coords = false ;
	}
	if( in_coords ) {
		var cols = data.split( /\s/ ) ;
		coords.push( { ix: Number.parseInt( cols[0] ), x: Number.parseFloat( cols[1] ), y: Number.parseFloat( cols[2] ) } ) ;
	}
	if( data==="NODE_COORD_SECTION") {
		in_coords = true ;
	}
}

var input = fs.createReadStream('../qa194.tsp');
//var input = fs.createReadStream('../sw24978.tsp');
readLines(input, func);


function TspAnneal( coords ) {
	this.coords = coords ; 

	this.initial_solution = function() {        
        return this.coords ;
    }

    /**
        how much does a solution cost. in this case it's path distance
    */
    this.solution_cost = function( solution ) {     
    	var dist = 0 ;
    	for( var i=1 ; i<coords.length ; i++ ) {
    		var dx = coords[i].x - coords[i-1].x ;
    		var dy = coords[i].y - coords[i-1].y ;
    		dist += Math.sqrt( dx*dx + dy*dy ) ;
    	}
    	return dist  ;
    }

    /**
        Make a random transition - in our case swap an item
        between one bin and another. For large scale solution states
        a better random number generator is recommended (It really is 
        important).

        Return the new solution
    */
    this.random_transition = function( solution ) {
        var rc = solution.slice( 0 );

        var bin1 = Math.floor( Math.random() * (solution.length-1) ) ;
        var bin2 = bin1 + 1 ;
        
        rc[bin1] = solution[bin2] ;
        rc[bin2] = solution[bin1] ;

        return rc ;
    }
}

TspAnneal.prototype = new algos.SimulatedAnnealing();  // Here's where the "inheritance" occurs 
TspAnneal.prototype.constructor=TspAnneal;            

function process() {
	if( !coords.length ) { throw new Error( "No data available." ) ; }
	var minx = coords.reduce( function(p,e) { return Math.min(p,e.x); }, 999999999 ) ;
	var maxx = coords.reduce( function(p,e) { return Math.max(p,e.x); }, -999999999 ) ;
	var miny = coords.reduce( function(p,e) { return Math.min(p,e.y); }, 999999999 ) ;
	var maxy = coords.reduce( function(p,e) { return Math.max(p,e.y); }, -999999999 ) ;
	
	var order = 20 ;
	var hilbert_length = Math.pow(2,order) ;
	var scalex = hilbert_length / ( maxx - minx );
	var scaley = hilbert_length / ( maxy - miny );
	for( var i=0 ; i<coords.length ; i++ ) {
		var scaled_x = ( coords[i].x - minx ) * scalex ;
		var scaled_y = ( coords[i].y - miny ) * scaley ;
//		console.log( scaled_x, ",", scaled_y ) ;
		coords[i].hilbert_index = hilbert.point_to_hilbert( scaled_x,scaled_y, order ) ;		
	}
	coords.sort( function(a,b){ return a.hilbert_index - b.hilbert_index } ) ;
	
	var tspAnnealer = new TspAnneal( coords ) ;
	var result = tspAnnealer.anneal()  ; 
	
	var dist = 0 ;
	for( var i=1 ; i<coords.length ; i++ ) {
		var dx = coords[i].x - coords[i-1].x ;
		var dy = coords[i].y - coords[i-1].y ;
		dist += Math.sqrt( dx*dx + dy*dy ) ;
	}
	console.log( dist ) ;
}
