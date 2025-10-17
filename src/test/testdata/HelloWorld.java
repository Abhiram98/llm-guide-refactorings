package org.boulderse.ijserver.testdata;

import java.util.List;

class HelloWorld{
    public static void main(String[] args){
        int x =1;
        System.out.println(x);
        System.out.println("Hello world");

    }

    public void linearSearch(List<Integer> array, int value){
        for (int i=0;i<array.size();i++){
            if (array.get(i) ==value)
                System.out.println("Found value.");
        }
    }

    public void prettyPrintInteger(Integer num){
        switch (num){
            case 1: System.out.println("ONE!!"); break;
            case 2: System.out.println("TWO!!"); break;
            case 5: System.out.println("FIVE!!"); break;
            default: System.out.println(num);
        }
    }
    public void prettyPrintIntegerIfImpl(Integer num){
        if (num==1){
            System.out.println("ONE!!");
        } else if (num==2) {
            System.out.println("TWO!!");
        } else if (num==5) {
            System.out.println("FIVE!!");
        } else {
            System.out.println(num);
        }
    }

    public Integer numMinus10(Integer num){
        if (num>=0)
            return num-10;
        else
            return num+10;
    }

    public Integer numMinus10Ternary(Integer num){
        return num >= 0 ? num - 10 : num + 10;
    }

    public static void prettyPrintArray(List<Integer> array){
        String result = "Array: ";

        for(int i = 0; i < array.size(); i++) {
            result += "Element: "+(i+1);
            result += array.get(i).toString();
            result += "\n";
        }
        System.out.println(result);
    }

    public static void constructString(Integer a, Boolean b){
        String s = "String: " + "s" + a + b;
        System.out.println(s);
    }

    public static void client(){
        constructString(1, false);
        constructString(3, true);
    }


}