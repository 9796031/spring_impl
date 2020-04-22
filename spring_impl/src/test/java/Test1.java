public class Test1 {

    public static void main(String[] args) {
        for (int i = 0;i<40000;i++) {
            testInt();
        }
    }

    public static void testInt() {
        int j = 7;
        while(j>0) {
            j = j - 3;
        }
        System.out.println(j);
    }
}
