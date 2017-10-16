package com.example.a6100890.gleaner;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by a6100890 on 2017/10/15.
 */

public class MessageFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private List<EMConversation> mConversationList = new ArrayList<>();
    private int mPostion;
    private Toolbar mToolbar;


    private static final String TAG = "MessageFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConversationList.addAll(loadConversationList());


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        initView(view);

        return view;
    }
    public static MessageFragment newInsatnce() {
        return new MessageFragment();
    }

    private void initView(View view) {
        mRecyclerView = view.findViewById(R.id.recycler_conversation);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new ConversationAdapter(mConversationList));
    }

    /**
     * 从chatManager中获取会话列表
     * @return
     */
    private List<EMConversation> loadConversationList() {
        Map<String, EMConversation> conversations = EMClient.getInstance().chatManager().getAllConversations();

        //过滤掉messages size 为零的conversation
        //如在排序过程中收到新消息会造成并发问题抛出异常
        List<Pair<Long, EMConversation>> sortList = new ArrayList<>();
        synchronized (conversations) {
            for (EMConversation conversation : conversations.values()) {
                if (conversation.getAllMessages().size() != 0) {
                    sortList.add(new Pair<>(conversation.getLastMessage().getMsgTime(), conversation));
                }
            }
        }

        try {
            sortConversationByLastChatTime(sortList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<EMConversation> list = new ArrayList<>();
        for (Pair<Long, EMConversation> sortItem : sortList) {
            list.add(sortItem.second);
        }

        return list;

    }

    /**
     * 根据最后一条消息的时间对conversation排序
     * 参数Pair用于关联EmConVersation 和 Long 时间
     * @param conversationList
     */
    private void sortConversationByLastChatTime(List<Pair<Long, EMConversation>> conversationList) {
        Collections.sort(conversationList, new Comparator<Pair<Long, EMConversation>>() {
            @Override
            public int compare(Pair<Long, EMConversation> con1, Pair<Long, EMConversation> con2) {
                if (con1.first == con2.first) {
                    return 0;
                } else if (con2.first > con1.first) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }


    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView unreadLaber;
        private TextView message;   //最后一条消息的内容
        private TextView time;      //最后一条消息的时间
        private View state;         //发送消息状态(比如无效感叹号)

        public ConversationViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            unreadLaber = itemView.findViewById(R.id.unread_msg_number);
            message = itemView.findViewById(R.id.message);
            time = itemView.findViewById(R.id.time);
            state = itemView.findViewById(R.id.msg_state);
        }
    }

    class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
        private List<EMConversation> mConversationList = new ArrayList<>();
        private boolean notifyBuFilter;

        public ConversationAdapter(List<EMConversation> conversationList) {
            mConversationList = conversationList;
        }

        @Override
        public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
            ConversationViewHolder viewHolder = new ConversationViewHolder(view);
            //设置点击事件
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EMConversation conversation = mConversationList.get(mPostion);
                    String username = conversation.conversationId();

                    Toast.makeText(getActivity(), username, Toast.LENGTH_SHORT).show();
                }
            });


            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ConversationViewHolder holder, int position) {
            EMConversation conversation = mConversationList.get(position);
            mPostion = position;

            String username = conversation.conversationId();
            holder.name.setText("与 " + username + " 的会话");
            if (conversation.getUnreadMsgCount() > 0) {
                //显示未读消息数
                holder.unreadLaber.setText(String.valueOf(conversation.getUnreadMsgCount()));
                holder.unreadLaber.setVisibility(View.VISIBLE);
            } else {
                holder.unreadLaber.setVisibility(View.INVISIBLE);
            }

            if (conversation.getAllMsgCount() != 0) {
                //把最后一条消息的内容作为item的message内容
                EMMessage lastMessage = conversation.getLastMessage();
                holder.message.setText(lastMessage.getBody().toString());
                holder.time.setText(DateUtils.getTimestampString(new Date(lastMessage.getMsgTime())));
                //设置失败感叹号是否显示
                if (lastMessage.direct() == EMMessage.Direct.SEND && lastMessage.status() == EMMessage.Status.FAIL) {
                    holder.state.setVisibility(View.VISIBLE);
                } else {
                    holder.state.setVisibility(View.GONE);
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mConversationList.size();
        }


    }




}
