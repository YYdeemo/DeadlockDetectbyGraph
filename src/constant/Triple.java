package constant;

public class Triple<A,B,C> {
    private A first;
    private B second;
    private C third;

    public Triple(A first, B second, C third) {
        super();
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public int hashCode() {					//?
        int hashFirst = first != null ? first.hashCode() : 0;
        int hashSecond = second != null ? second.hashCode() : 0;
        int hashThird = third != null ? third.hashCode() : 0;

        return hashFirst*3+hashSecond*5+hashThird*7;
    }

    public boolean equals(Object other) {
        if (other instanceof Triple) {
            Triple otherTriple = (Triple) other;
            return
                    ((  this.first == otherTriple.first ||
                            ( this.first != null && otherTriple.first != null &&			//?
                                    this.first.equals(otherTriple.first))) &&
                            (	this.second == otherTriple.second ||
                                    ( this.second != null && otherTriple.second != null &&		//?
                                            this.second.equals(otherTriple.second))) &&
                            (	this.third == otherTriple.third ||
                                    ( this.third != null && otherTriple.third != null &&		//?
                                            this.third.equals(otherTriple.third))) );
        }

        return false;
    }

    public String toString()
    {
        return "(" + first + "," + second + ","+third+")";
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    public C getThird(){
        return third;
    }

    public void setThird(C third){
        this.third = third;
    }

    public int getintfirst() {
        int num =  Integer.parseInt(first.toString());
        return num;
    }

    public int getintsecond() {
        int num =  Integer.parseInt(second.toString());
        return num;

    }

    public int getinttird() {
        int num =  Integer.parseInt(third.toString());
        return num;

    }

}
