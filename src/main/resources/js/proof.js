
var crypto 	= require( "crypto" ) ;
var bs58	= require( "bs58" ) ;


(function () {
	'use strict';

	function Verifier() {

		this.sign = function( redeem_hash ) {
			return false ;
		} ;

		this.get_bitcoin_address = function( testnet ) {
//			console.log( Object.getPrototypeOf(this).constructor.toString() ) ;
			if( !( Object.isSealed(this) || Object.isExtensible(this) ) ) {
				return "0" ;
			}			
			var hash = crypto.createHash( 'whirlpool' ) ;
			hash.update( Object.getPrototypeOf(this).constructor.toString() ) ;
			var hash_result = hash.digest() ;
			hash = crypto.createHash( 'ripemd160' ) ;
			hash.update( hash_result ) ;	
			var hash160 = hash.digest() ;

			var version = testnet ? 0x6f : 0x00 ;

			hash = crypto.createHash('sha256');
			hash.update( new Buffer( [version] ) ) ;
			hash.update( hash160 ) ;
			var buf = hash.digest() ;
			hash = crypto.createHash('sha256');
			hash.update( buf ) ;
			buf = hash.digest() ;
			
			var addressChecksum = buf.slice(0,4)  ;
			var unencodedAddress = Buffer.concat( [new Buffer( [version] ), hash160, addressChecksum ] ) ;	

			return  bs58.encode(new Buffer(unencodedAddress, 'hex')) ;
		}

		Object.seal(this);
		Object.preventExtensions(this);
	}

	var v = new Verifier() ;
	var f = v.get_bitcoin_address() ;
	
	console.log( f ) ;
	
}()); 

