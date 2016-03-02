package utils;

public class AddedNodes{
	public int in;
	public int out;
	public AddedNodes(){
		this.in=0;
		this.out=0;
	}
	public String toString() {
		String string = new String();
		string+="{"+this.in+";"+this.out+"}";
		return string;
	}
}
