package jp.techacademy.takuya.hatakeyama2.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private Button favoriteButton;

    private DatabaseReference mAnswerRef;
    private DatabaseReference mDatabaseReference;
    private boolean mFavoriteRegisteredFlag;
    private DatabaseReference mFavoriteUserListReference;
    private String currentUserId;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for (Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        favoriteButton = (Button) findViewById(R.id.favorite_button);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mFavoriteUserListReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ArrayList<String> userIdSet = (ArrayList<String>) dataSnapshot.getValue();
                        if (mFavoriteRegisteredFlag) {
                            //お気にいり登録されている場合にはお気に入りから削除
                            userIdSet.remove(currentUserId);
                            Toast.makeText(QuestionDetailActivity.this, "お気にいり登録が解除されました。", Toast.LENGTH_SHORT).show();
                            mFavoriteUserListReference.removeValue();
                            mFavoriteUserListReference.setValue(userIdSet);
                            mFavoriteRegisteredFlag = false;
                        } else {
                            //お気にいり登録されていない場合にはお気に入りに登録
                            userIdSet.add(currentUserId);
                            Toast.makeText(QuestionDetailActivity.this, "お気にいりに登録されました。", Toast.LENGTH_SHORT).show();
                            mFavoriteUserListReference.removeValue();
                            mFavoriteUserListReference.setValue(userIdSet);
                            mFavoriteRegisteredFlag = true;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                renderButton();
            }
        });

        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            //現在ログイン中のユーザー情報を取得
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            //質問がログイン中のユーザーのお気にいりに登録されているかを判定
            mFavoriteUserListReference = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child("favoriteUserList");
            mFavoriteUserListReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<String> userIdSet = (ArrayList<String>) dataSnapshot.getValue();
                    if (userIdSet.contains(currentUserId)) {
                        //お気にいり登録されている
                        mFavoriteRegisteredFlag = true;
                    } else {
                        //お気にいり登録されていない
                        mFavoriteRegisteredFlag = false;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        renderButton();
    }

    private void renderButton() {
        //ログインしていなければお気にいりボタンを非表示
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            favoriteButton.setVisibility(View.INVISIBLE);
            favoriteButton.setEnabled(false);
        } else {
            favoriteButton.setVisibility(View.VISIBLE);
            favoriteButton.setEnabled(true);

            //お気に入り登録有無に応じて、ボタンのテキストを変更する。
            mFavoriteUserListReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (mFavoriteRegisteredFlag) {
                        //お気にいり登録されている
                        favoriteButton.setText("お気に入り登録済み");
                    } else {
                        //お気にいり登録されていない
                        favoriteButton.setText("お気に入り登録前");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
