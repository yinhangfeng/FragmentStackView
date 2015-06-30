package com.example.yinhangfeng.fragmentstackview.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.yinhangfeng.fragmentstackview.FragmentStackView;


public class MainActivity extends AppCompatActivity {

    private FragmentStackView fragmentStackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentStackView = (FragmentStackView) findViewById(R.id.fragment_stack_view);
        fragmentStackView.setFragmentManager(getSupportFragmentManager());
        fragmentStackView.setShadow(R.drawable.left_edge_shadow);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
        case R.id.test1:
            test1();
            break;
        case R.id.test2:
            test2();
            break;
        case R.id.test3:
            test3();
            break;
        case R.id.test4:
            test4();
            break;
        case R.id.test5:
            test5();
            break;
        case R.id.test6:
            test6();
            break;
        case R.id.test7:
            test7();
            break;
        case R.id.test8:
            test8();
            break;
        case R.id.test9:
            test9();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        fragmentStackView.pop();
    }

    private void test1() {
        fragmentStackView.push(new Fragment1(), "aaa", true);
    }

    private void test2() {
        fragmentStackView.push(new Fragment1(), "bbb", false);
    }

    private void test3() {
        fragmentStackView.pop(true);
    }

    private void test4() {
        fragmentStackView.pop(false);
    }

    private void test5() {
        fragmentStackView.popByIndex(0, false, true);
    }

    private void test6() {
        fragmentStackView.popByIndex(0, false, false);
    }

    private void test7() {
        fragmentStackView.popByIndex(0, true, true);
    }

    private void test8() {
        fragmentStackView.popByIndex(1, true, true);
    }

    private void test9() {
        fragmentStackView.debug();
    }
}
