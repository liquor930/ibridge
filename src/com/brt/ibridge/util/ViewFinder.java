package com.brt.ibridge.util;

import android.view.View;

/**
 * 类型安全 View 查找包装，替代 findViewById + 强制转换
 * 后续可替换为 Android ViewBinding 生成的类
 */
public class ViewFinder {
    private final View root;

    public ViewFinder(View root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T id(int id) {
        return (T) root.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T view(View root, int id) {
        return (T) root.findViewById(id);
    }
}
