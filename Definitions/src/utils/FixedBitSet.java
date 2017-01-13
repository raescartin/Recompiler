/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/

package utils;

import java.util.BitSet;

public class FixedBitSet extends BitSet{

    /**
	 * Default size is 64 bits
	 */
	private static final long serialVersionUID = 2182495250198420548L;
	int fixedLength;
	
	public FixedBitSet(){
    }
	
    public FixedBitSet(int fixedLength){
        super(fixedLength);
        this.fixedLength = fixedLength;
    }

    @Override
    public int length() {
        return fixedLength;
    }
    
    public FixedBitSet nand(FixedBitSet in){
    	FixedBitSet outFBS=(FixedBitSet) this.clone();
		outFBS.and(in);
		outFBS.flip(0,outFBS.size());
		outFBS.fixedLength=Integer.max(this.length(), in.length());
//		outFBS.flip(0,outFBS.length());
		return outFBS;
    }
    public static FixedBitSet fromString(final String binary) {
		FixedBitSet bits = new FixedBitSet(binary.length());
		for(int i=0; i<binary.length(); i++){
				bits.set(i,binary.charAt(i) == '1');
		}
		return bits;
    }

    public String toString() {
        String string = new String();
	        for(int i=0; i<this.length(); i++){
				if( this.get(i) ) {
					string+='1';
				}else{
					string+='0';
				}
			}
        return string;
    }
    public void concat(FixedBitSet fbs){
    	int length=this.length();
    	if(fbs!=null){
	    	this.fixedLength=this.length()+fbs.length();
	    	for(int i=0; i<fbs.length(); i++){
				if( fbs.get(i) ) {
					this.set(length+i);
				}
	    	}
    	}
    }
    
    public FixedBitSet get(int x, int y){
    	FixedBitSet fbs = new FixedBitSet();
    	if(x<=y){
	    	fbs = new FixedBitSet(y-x+1);
	    	int j=0;
	    	for(int i=x; i<=y; i++){
				if( this.get(i) ) {
					fbs.set(j);
				}
				j++;
			}
    	}
	    return fbs;
		//return (FixedBitSet) super.get(i, j);
	}
//    public void setLength(int fixedLength){
//        this.fixedLength = fixedLength;
//    }
}
