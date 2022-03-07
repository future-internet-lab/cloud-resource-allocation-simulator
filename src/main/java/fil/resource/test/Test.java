package fil.resource.test;

interface MyInterface {
    public void method1();
    public void method2();
}

class MyClass implements MyInterface {
    public void method1() {
        System.out.println( "method 1" );
    }
}

public class Test {
    public static void main( String args[] ) {
        MyClass c1 = new MyClass();
        c1.method1();
        c1.method2();
    }
}