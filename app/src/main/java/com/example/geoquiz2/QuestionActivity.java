package com.example.geoquiz2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.geoquiz2.Adapter.AnswerSheetAdapter;
import com.example.geoquiz2.Adapter.AnswerSheetHelperAdapter;
import com.example.geoquiz2.Adapter.QuestionFragmentAdapter;
import com.example.geoquiz2.Common.Common;
import com.example.geoquiz2.DBHelper.DBHelper;
import com.example.geoquiz2.Model.CurrentQuestion;
import com.example.geoquiz2.Model.Question;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

import static androidx.navigation.Navigation.findNavController;

public class QuestionActivity extends AppCompatActivity {

    private static final int CODE_GET_RESULT = 9999;
    private AppBarConfiguration mAppBarConfiguration;

    int time_play = Common.TOTAL_TIME;
    boolean  isAnswerModeView = false;

    TextView txt_right_answer,txt_timer,txt_wrong_answer;

    RecyclerView answer_sheet_view;
    AnswerSheetAdapter answerSheetAdapter;
    AnswerSheetHelperAdapter answerSheetHelperAdapter;

    ViewPager viewPager;
    TabLayout tabLayout;

    ConstraintLayout constraintLayout;

    @Override
    protected void onDestroy() {
        if(Common.countDownTimer!=null)
            Common.countDownTimer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_question);
       Toolbar toolbar = findViewById(R.id.toolbar);
       toolbar.setTitle(Common.selectedCategory.getName());
       setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //First, take question from DB
        takeQuestion();

        if(Common.questionList.size()>0) {

            //Show text view right answer and text view timer

            txt_right_answer = findViewById(R.id.txt_question_right);
            txt_timer = findViewById(R.id.txt_timer);

            txt_timer.setVisibility(View.VISIBLE);
            txt_right_answer.setVisibility(View.VISIBLE);

            txt_right_answer.setText(new StringBuilder(String.format("%d/%d",Common.right_answer_count,Common.questionList.size())));
            
            countTimer();

            answer_sheet_view = findViewById(R.id.grid_answer);
            answer_sheet_view.setHasFixedSize(true);
            if (Common.questionList.size() > 5)
                answer_sheet_view.setLayoutManager(new GridLayoutManager(this, Common.questionList.size() / 2));
            answerSheetAdapter = new AnswerSheetAdapter(this, Common.answerSheetList);
            answer_sheet_view.setAdapter(answerSheetAdapter);


            viewPager=findViewById(R.id.viewpager);
            tabLayout=findViewById(R.id.sliding_tabs);

            genFragmentList();


            QuestionFragmentAdapter questionFragmentAdapter = new QuestionFragmentAdapter(getSupportFragmentManager(),this,Common.fragmentsList);
            viewPager.setAdapter(questionFragmentAdapter);
            tabLayout.setupWithViewPager(viewPager);

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {


                int SCROLLING_RIGHT = 0;
                int SCROLLING_LEFT = 1;
                int SCROLLING_UNDETERMINED = 2;

                int currentScrollDirection = 2;

                private void setScrollingDirection(float positionOffset){
                    if(1-positionOffset >= 0.5){
                        this.currentScrollDirection = SCROLLING_RIGHT;
                    }
                    else if(1-positionOffset <=0.5){
                        this.currentScrollDirection = SCROLLING_LEFT;
                    }

                }

                private boolean isScrolledDirectionUndetermined(){
                    return currentScrollDirection == SCROLLING_UNDETERMINED;
                }

                private boolean isScrolledRight(){
                    return currentScrollDirection == SCROLLING_RIGHT;
                }
                private boolean isScrolledLeft(){
                    return currentScrollDirection == SCROLLING_LEFT;
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    if(isScrolledDirectionUndetermined()){
                        setScrollingDirection(positionOffset);
                    }
                }

                @Override
                public void onPageSelected(int position) {

                    QuestionFragment questionFragment;
                    int i = 0 ;
                    if(position>0){
                            if(isScrolledRight()){
                                questionFragment = Common.fragmentsList.get(position-1);
                                i = position - 1;
                            }
                            else if(isScrolledLeft()){
                                //If user scrolls left then get next fragment to calculate result
                                questionFragment = Common.fragmentsList.get(position+1);
                                i = position + 1;
                            }
                            else{
                                questionFragment = Common.fragmentsList.get(i);
                            }
                    }
                    else{
                        questionFragment = Common.fragmentsList.get(0);
                        i=0;
                    }

                    CurrentQuestion question_state = questionFragment.getSelectedAnswer();
                    Common.answerSheetList.set(i,question_state); // Set question's answer for answersheet
                    answerSheetAdapter.notifyDataSetChanged(); //change color in answer sheet

                    countCorrectAnswer();

                    txt_right_answer.setText(new StringBuilder(String.format("%d",Common.right_answer_count))
                            .append("/")
                            .append(String.format("%d",Common.questionList.size())).toString());

                    txt_wrong_answer.setText(String.valueOf(Common.wrong_answer_count));

                    if(question_state.getType()==Common.ANSWER_TYPE.NO_ANSWER){
                        questionFragment.showCorrectAnswer();
                        questionFragment.disableAnswer();
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                    if(state == ViewPager.SCROLL_STATE_IDLE){
                        this.currentScrollDirection = SCROLLING_UNDETERMINED;
                    }

                }

            });


        }

    }

    private void finishGame() {
        int i = viewPager.getCurrentItem();
        QuestionFragment questionFragment = Common.fragmentsList.get(i);
        CurrentQuestion question_state = questionFragment.getSelectedAnswer();
        Common.answerSheetList.set(i,question_state); // Set question's answer for answersheet
        answerSheetAdapter.notifyDataSetChanged(); //change color in answer sheet

        countCorrectAnswer();

        txt_right_answer.setText(new StringBuilder(String.format("%d",Common.right_answer_count))
                .append("/")
                .append(String.format("%d",Common.questionList.size())).toString());
        txt_wrong_answer.setText(String.valueOf(Common.wrong_answer_count));

        if(question_state.getType()==Common.ANSWER_TYPE.NO_ANSWER){
            questionFragment.showCorrectAnswer();
            questionFragment.disableAnswer();
        }

        //Navigate to result activity

        Intent intent = new Intent(this,ResultActivity.class);
        Common.timer = Common.TOTAL_TIME - time_play;
        Common.no_answer_count = Common.questionList.size() - (Common.wrong_answer_count+Common.right_answer_count);
        Common.data_question = new StringBuilder(new Gson().toJson(Common.answerSheetList));

        startActivityForResult(intent,CODE_GET_RESULT);

    }

    private void countCorrectAnswer() {
        //reset variable
        Common.right_answer_count = Common.wrong_answer_count = 0;
        for(CurrentQuestion item:Common.answerSheetList){
            if(item.getType()==Common.ANSWER_TYPE.WRONG_ANSWER)
                Common.wrong_answer_count++;
             else if(item.getType()==Common.ANSWER_TYPE.RIGHT_ANSWER)
                Common.right_answer_count++;
        }
    }

    private void genFragmentList() {
        for(int i=0;i<Common.questionList.size();i++){
            Bundle bundle = new Bundle();
            bundle.putInt("index",i);
            QuestionFragment fragment = new QuestionFragment();
            fragment.setArguments(bundle);

            Common.fragmentsList.add(fragment);
        }
    }

    private void countTimer() {
        if(Common.countDownTimer ==  null){
            Common.countDownTimer = new CountDownTimer(Common.TOTAL_TIME,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    txt_timer.setText(String.format("02%d:02%d",
                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)-
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));

                    time_play-=1000;

                }

                @Override
                public void onFinish() {

                    finishGame();
                }
            }.start();
        }

        else{
            Common.countDownTimer.cancel();
            Common.countDownTimer = new CountDownTimer(Common.TOTAL_TIME,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    txt_timer.setText(String.format("02%d:02%d",
                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)-
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));

                    time_play-=1000;

                }

                @Override
                public void onFinish() {



                }
            }.start();

        }
    }

    private void takeQuestion() {
        Common.questionList = DBHelper.getInstance(this).getQuestionByCategory(Common.selectedCategory.getId());
        if(Common.questionList.size()==0){
            new MaterialStyledDialog.Builder(this).setTitle("OOPS!!").setIcon(R.drawable.ic_sentiment_very_dissatisfied_black_24dp)
            .setDescription("We don't have any question in this "+Common.selectedCategory.getName()+" category")
            .setPositiveText("OK");
        }
        else{
            if(Common.answerSheetList.size()>0)
                Common.answerSheetList.clear();
            for(int i=0;i<Common.questionList.size();i++){
                Common.answerSheetList.add(new CurrentQuestion(i, Common.ANSWER_TYPE.NO_ANSWER));
            }
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_wrong_answer);
//        constraintLayout =(ConstraintLayout) item.getActionView();
//        txt_wrong_answer = (TextView) constraintLayout.findViewById(R.id.txt_wrong_answer);
//        txt_wrong_answer.setText(String.valueOf(0));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menu_wrong_answer);
        //constraintLayout =(ConstraintLayout) item.getActionView();
        //txt_wrong_answer = (TextView) constraintLayout.findViewById(R.id.txt_wrong_answer);
        //txt_wrong_answer.setText(String.valueOf(0));
        getMenuInflater().inflate(R.menu.question, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.menu_finish_game){
            if(!isAnswerModeView){
                new MaterialStyledDialog.Builder(this)
                        .setTitle("Finish?")
                        .setIcon(R.drawable.ic_mood_black_24dp)
                        .setDescription("Do you really want to finish?")
                        .setNegativeText("No")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveText("Yes")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                                finishGame();
                            }
                        }).show();
            }
            else{
                finishGame();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CODE_GET_RESULT){
            if(resultCode == Activity.RESULT_OK){
                String action = data.getStringExtra("action");
                if(action == null || TextUtils.isEmpty(action)){
                    int questionNum = data.getIntExtra(Common.KEY_BACK_FROM_RESULT,-1);
                    viewPager.setCurrentItem(questionNum);

                    isAnswerModeView = true;
                    Common.countDownTimer.cancel();

                    txt_wrong_answer.setVisibility(View.GONE);
                    txt_right_answer.setVisibility(View.GONE);
                    txt_timer.setVisibility(View.GONE);
                }
                else{
                    if(action.equals("viewquizanswer")){
                        viewPager.setCurrentItem(0);

                        isAnswerModeView = true;

                        Common.countDownTimer.cancel();

                        txt_wrong_answer.setVisibility(View.GONE);
                        txt_right_answer.setVisibility(View.GONE);
                        txt_timer.setVisibility(View.GONE);

                        for(int i=0;i<Common.fragmentsList.size();i++){
                            Common.fragmentsList.get(i);
                            Common.fragmentsList.get(i).disableAnswer();

                        }
                    }
                    else if(action.equals("doitagain")){

                        viewPager.setCurrentItem(0);

                        isAnswerModeView = false;

                        countTimer();

                        txt_wrong_answer.setVisibility(View.VISIBLE);
                        txt_right_answer.setVisibility(View.VISIBLE);
                        txt_timer.setVisibility(View.VISIBLE);

                        for(CurrentQuestion item:Common.answerSheetList)
                            item.setType(Common.ANSWER_TYPE.NO_ANSWER); //Reset All questions

                        answerSheetAdapter.notifyDataSetChanged();
                        answerSheetHelperAdapter.notifyDataSetChanged();

                        for(int i = 0 ;i<Common.fragmentsList.size();i++)
                            Common.fragmentsList.get(i).resetQuestion();

                    }
                }
            }
        }
    }
}
