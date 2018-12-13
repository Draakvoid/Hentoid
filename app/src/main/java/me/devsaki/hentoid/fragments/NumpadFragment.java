package me.devsaki.hentoid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.devsaki.hentoid.R;

public final class NumpadFragment extends Fragment {

    private Parent parent;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parent = (Parent) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_numpad, container, false);
        bindViewClick(view, R.id.button_0, () -> parent.onKeyClick("0"));
        bindViewClick(view, R.id.button_1, () -> parent.onKeyClick("1"));
        bindViewClick(view, R.id.button_2, () -> parent.onKeyClick("2"));
        bindViewClick(view, R.id.button_3, () -> parent.onKeyClick("3"));
        bindViewClick(view, R.id.button_4, () -> parent.onKeyClick("4"));
        bindViewClick(view, R.id.button_5, () -> parent.onKeyClick("5"));
        bindViewClick(view, R.id.button_6, () -> parent.onKeyClick("6"));
        bindViewClick(view, R.id.button_7, () -> parent.onKeyClick("7"));
        bindViewClick(view, R.id.button_8, () -> parent.onKeyClick("8"));
        bindViewClick(view, R.id.button_9, () -> parent.onKeyClick("9"));
        bindViewClick(view, R.id.button_backspace, () -> parent.onBackspaceClick());
        return view;
    }

    private static void bindViewClick(View parentView, @IdRes int viewId, Runnable runnable) {
        View view = ViewCompat.requireViewById(parentView, viewId);
        view.setOnClickListener(v -> runnable.run());
    }

    public interface Parent {

        void onKeyClick(String s);

        void onBackspaceClick();
    }
}
