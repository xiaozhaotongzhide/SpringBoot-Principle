package com.example.a13;

import org.springframework.cglib.core.Signature;

public class TargetFastClass {

    static Signature s0 = new Signature("save","()V");
    static Signature s1 = new Signature("save","(I)V");
    static Signature s2 = new Signature("save","(J)V");

    //获取目标方法的编号
    public int getIndex(Signature signature) {
        if (s0.equals(signature)){
            return 0;
        } else if (s1.equals(signature)) {
            return 1;
        } else if (s2.equals(signature)) {
            return 2;
        } else {
            throw new RuntimeException("without this parameter");
        }
    }

    public Object invoke(int index,Object target,Object[] args) {
        if (index == 0){
            ((Target) target).save();
            return null;
        } else if (index == 1) {
            ((Target) target).save((int) args[0]);
            return null;
        } else if (index == 2) {
            ((Target) target).save((long) args[0]);
            return null;
        } else {
            throw new RuntimeException("without this parameter");
        }
    }

    public static void main(String[] args) {
        TargetFastClass targetFastClass = new TargetFastClass();
        int index = targetFastClass.getIndex(new Signature("save", "()V"));
        targetFastClass.invoke(index,new Target(),new Object[0]);
    }
}
