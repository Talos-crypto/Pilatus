package mopetree;

/** Simple class for joining two objects of different type */
public class Tuple<A,B> {
	public A a;
	public B b;
	
	public Tuple(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	public String toString() {
		return "(" + a.toString() + ", " + b.toString() + ")";
	}
}
