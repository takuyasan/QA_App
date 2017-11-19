package jp.techacademy.takuya.hatakeyama2.qa_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        // ログイン済みのユーザーのユーザーIDを取得する
        final String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //お気に入りリストビュー
        final ArrayList<Question> favoriteQuestionList = new ArrayList<>();
        final ListView favoriteListView = (ListView) findViewById(R.id.favorite_listView);
        final QuestionsListAdapter questionsListAdapter = new QuestionsListAdapter(this);
        questionsListAdapter.setQuestionArrayList(favoriteQuestionList);
        favoriteListView.setAdapter(questionsListAdapter);

        //ログイン中のユーザーのお気に入りに登録されている質問を取得するための前準備
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference contentsReference = databaseReference.child(Const.ContentsPATH);

        //JanreIdは１～４まで対応
        for (int i = 1; i <= 4; i++) {
            //JanreIDを匿名クラス内で参照するため変数に格納
            final int ganreID = i;
            final DatabaseReference janreReference = contentsReference.child(String.valueOf(i));
            janreReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    HashMap map = (HashMap) dataSnapshot.getValue();
                    ArrayList<String> favoriteUserList = (ArrayList<String>) map.get("favoriteUserList");
                    if (favoriteUserList.contains(currentUserId)) {
                        //質問がお気に入りに登録されている場合
                        String questionId = (String) map.get("uid");
                        String title = (String) map.get("title");
                        String body = (String) map.get("body");
                        String name = (String) map.get("name");
                        String uid = (String) map.get("uid");
                        String imageString = (String) map.get("image");

                        byte[] bytes;
                        if (imageString != null) {
                            bytes = Base64.decode(imageString, Base64.DEFAULT);
                        } else {
                            bytes = new byte[0];
                        }

                        Question question = new Question(title, body, name, uid, questionId, ganreID, bytes, new ArrayList<Answer>());
                        favoriteQuestionList.add(question);
                        questionsListAdapter.notifyDataSetChanged();
                    }

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
            });
        }
    }
}
