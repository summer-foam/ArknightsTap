package com.icebem.akt.model;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.snackbar.Snackbar;
import com.icebem.akt.R;
import com.icebem.akt.app.PreferenceManager;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RecruitViewer {
    private static final int TAG_CHECKED_MAX = 5;
    private static final int TAG_COMBINED_MAX = 3;
    private static final int CHECKED_TIME_ID = R.id.tag_time_3;
    private static final String FLAG_UNRELEASED = "*";
    private static final int[][] CHECKED_STARS_ID = {
            {R.id.tag_time_3, R.id.tag_star_3, R.id.tag_star_4, R.id.tag_star_5},
            {R.id.tag_time_2, R.id.tag_star_2, R.id.tag_star_3, R.id.tag_star_4, R.id.tag_star_5},
            {R.id.tag_time_1, R.id.tag_star_1, R.id.tag_star_2, R.id.tag_star_3, R.id.tag_star_4}
    };
    private boolean autoAction;
    private Context context;
    private TextView tip;
    private NestedScrollView scroll;
    private ViewGroup tagsContainer, resultContainer;
    private PreferenceManager manager;
    private OperatorInfo[] infoList;
    private ArrayList<CheckBox> stars, qualifications, types, checkedStars, checkedTags, combinedTags;
    private ArrayList<OperatorInfo> checkedInfoList;
    private ArrayList<ItemContainer> resultList;

    public RecruitViewer(Context context, View root) throws IOException, JSONException {
        this.context = context;
        manager = new PreferenceManager(context);
        scroll = root.findViewById(R.id.scroll_recruit_root);
        tip = root.findViewById(R.id.txt_recruit_tips);
        resultContainer = root.findViewById(R.id.container_recruit_result);
        tagsContainer = root.findViewById(R.id.container_recruit_tags);
        stars = findBoxesById(R.id.tag_star_1);
        qualifications = findBoxesById(R.id.tag_qualification_1);
        types = findBoxesById(R.id.tag_type_vanguard);
        if (!(context instanceof AppCompatActivity))
            tip.setOnClickListener(view -> tagsContainer.setVisibility(tagsContainer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE));
        root.findViewById(R.id.action_recruit_reset).setOnClickListener(RecruitViewer.this::resetTags);
        ((RadioGroup) tagsContainer.findViewById(R.id.group_recruit_time)).setOnCheckedChangeListener(this::onCheckedChange);
        setOnCheckedChangeListener(stars);
        setOnCheckedChangeListener(qualifications);
        setOnCheckedChangeListener(findBoxesById(R.id.tag_position_melee));
        setOnCheckedChangeListener(types);
        setOnCheckedChangeListener(findBoxesById(R.id.tag_affix_survival));
        infoList = OperatorInfo.fromAssets(context);
        checkedStars = new ArrayList<>();
        checkedTags = new ArrayList<>();
        checkedInfoList = new ArrayList<>();
        combinedTags = new ArrayList<>();
        resetTags(null);
    }

    private CheckBox findBoxById(int id) {
        return tagsContainer.findViewById(id);
    }

    private ArrayList<CheckBox> findBoxesById(int id) {
        ArrayList<CheckBox> boxes = new ArrayList<>();
        ViewGroup group = (ViewGroup) tagsContainer.findViewById(id).getParent();
        for (int i = 0; i < group.getChildCount(); i++)
            if (group.getChildAt(i) instanceof CheckBox)
                boxes.add((CheckBox) group.getChildAt(i));
        return boxes;
    }

    private void setOnCheckedChangeListener(ArrayList<CheckBox> boxes) {
        for (CheckBox box : boxes)
            box.setOnCheckedChangeListener(this::onCheckedChange);
    }

    private void onCheckedChange(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.group_recruit_time) {
            if (!autoAction)
                autoAction = true;
            while (!checkedStars.isEmpty())
                checkedStars.get(0).setChecked(false);
            for (int[] stars : CHECKED_STARS_ID) {
                if (stars[0] == checkedId)
                    for (int i = 1; i < stars.length; i++)
                        findBoxById(stars[i]).setChecked(true);
            }
            if (findBoxById(R.id.tag_qualification_6).isChecked())
                findBoxById(R.id.tag_star_6).setChecked(true);
            autoAction = false;
            updateRecruitResult();
        }
    }

    private void onCheckedChange(CompoundButton tag, boolean isChecked) {
        if (tag instanceof CheckBox) {
            if (!stars.contains(tag) && isChecked && checkedTags.size() >= TAG_CHECKED_MAX) {
                tag.setChecked(false);
            } else {
                if (tag.getId() == R.id.tag_qualification_6 && findBoxById(R.id.tag_star_6).isChecked() != isChecked) {
                    autoAction = true;
                    findBoxById(R.id.tag_star_6).setChecked(isChecked);
                    autoAction = false;
                }
                updateCheckedTags((CheckBox) tag, isChecked);
            }
        }
    }

    public void resetTags(@Nullable View view) {
        if (view != null)
            view.setClickable(false);
        autoAction = true;
        if (tagsContainer.getVisibility() != View.VISIBLE)
            tagsContainer.setVisibility(View.VISIBLE);
        while (!checkedTags.isEmpty())
            checkedTags.get(0).setChecked(false);
        RadioButton timeTag = tagsContainer.findViewById(CHECKED_TIME_ID);
        if (timeTag.isChecked())
            onCheckedChange((RadioGroup) timeTag.getParent(), CHECKED_TIME_ID);
        else
            timeTag.setChecked(true);
        if (view != null)
            view.setClickable(true);
    }

    private void updateCheckedTags(CheckBox tag, boolean isChecked) {
        if (stars.contains(tag)) {
            if (isChecked)
                checkedStars.add(tag);
            else
                checkedStars.remove(tag);
            for (OperatorInfo info : infoList) {
                if (tag.getText().toString().contains(String.valueOf(info.getStar())) && (manager.recruitPreview() || !info.getName().endsWith(FLAG_UNRELEASED))) {
                    if (isChecked)
                        checkedInfoList.add(info);
                    else
                        checkedInfoList.remove(info);
                }
            }
            Collections.sort(checkedInfoList, this::compareInfo);
        } else {
            if (isChecked)
                checkedTags.add(tag);
            else
                checkedTags.remove(tag);
        }
        if (!autoAction)
            updateRecruitResult();
    }

    private void updateRecruitResult() {
        resultContainer.removeAllViews();
        if (checkedTags.isEmpty()) {
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            LinearLayout layout = new LinearLayout(context);
            for (OperatorInfo info : checkedInfoList)
                if (info.getStar() != 6)
                    layout.addView(getInfoView(info, layout));
            scroll.addView(layout);
            resultContainer.addView(scroll);
            tip.setText(checkedInfoList.isEmpty() ? R.string.tip_recruit_result_none : R.string.tip_recruit_result_default);
        } else {
            resultList = new ArrayList<>();
            Collections.sort(checkedTags, this::compareTags);
            for (int i = Math.min(checkedTags.size(), TAG_COMBINED_MAX); i > 0; i--)
                combineTags(0, combinedTags.size(), i);
            Collections.sort(resultList);
            for (ItemContainer container : resultList)
                resultContainer.addView(container);
            if (checkedTags.size() == TAG_CHECKED_MAX && manager.scrollToResult())
                scroll.post(() -> scroll.smoothScrollTo(0, tagsContainer.getHeight()));
            switch (resultList.isEmpty() ? 0 : resultList.get(0).getMinStar()) {
                case 6:
                    tip.setText(R.string.tip_recruit_result_6);
                    tip.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    tip.setMarqueeRepeatLimit(-1);
                    tip.setSingleLine(true);
                    tip.setSelected(true);
                    tip.setFocusable(true);
                    tip.setFocusableInTouchMode(true);
                    break;
                case 5:
                    tip.setText(R.string.tip_recruit_result_5);
                    break;
                case 4:
                    tip.setText(R.string.tip_recruit_result_4);
                    break;
                case 0:
                    tip.setText(checkedTags.contains(findBoxById(R.id.tag_qualification_1)) ? R.string.tip_recruit_result_1 : R.string.tip_recruit_result_none);
                    break;
                default:
                    tip.setText(checkedTags.contains(findBoxById(R.id.tag_qualification_1)) ? R.string.tip_recruit_result_1 : R.string.tip_recruit_result_default);
            }
        }
    }

    private void combineTags(int index, int size, int targetSize) {
        if (size == targetSize) {
            matchInfoList();
        } else {
            for (int i = index; i < checkedTags.size(); i++) {
                if (!combinedTags.contains(checkedTags.get(i))) {
                    combinedTags.add(checkedTags.get(i));
                    combineTags(i, combinedTags.size(), targetSize);
                    combinedTags.remove(checkedTags.get(i));
                }
            }
        }
    }

    private void matchInfoList() {
        ArrayList<OperatorInfo> matchedInfoList = new ArrayList<>();
        for (OperatorInfo info : checkedInfoList) {
            boolean matched = info.getStar() != 6 || combinedTags.contains(findBoxById(R.id.tag_qualification_6));
            for (CheckBox tag : combinedTags) {
                if (matched) {
                    if (qualifications.contains(tag)) {
                        matched = (tag.getId() == R.id.tag_qualification_1 && info.getStar() == 1) || (tag.getId() == R.id.tag_qualification_2 && info.getStar() == 2) || (tag.getId() == R.id.tag_qualification_5 && info.getStar() == 5) || (tag.getId() == R.id.tag_qualification_6 && info.getStar() == 6);
                    } else if (types.contains(tag)) {
                        matched = tag.getText().toString().equals(info.getType());
                    } else {
                        matched = info.containsTag(tag.getText().toString());
                    }
                } else break;
            }
            if (matched) matchedInfoList.add(info);
        }
        if (!matchedInfoList.isEmpty()) addResultToList(matchedInfoList);
    }

    private void addResultToList(ArrayList<OperatorInfo> matchedInfoList) {
        LinearLayout tagContainer = new LinearLayout(context);
        for (CheckBox box : combinedTags)
            tagContainer.addView(getTagView(box, tagContainer));
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        LinearLayout infoContainer = new LinearLayout(context);
        for (OperatorInfo info : matchedInfoList)
            infoContainer.addView(getInfoView(info, infoContainer));
        scroll.addView(infoContainer);
        ItemContainer itemContainer = new ItemContainer();
        itemContainer.setStar(Math.min(matchedInfoList.get(0).getStar(), matchedInfoList.get(matchedInfoList.size() - 1).getStar()), Math.max(matchedInfoList.get(0).getStar(), matchedInfoList.get(matchedInfoList.size() - 1).getStar()));
        itemContainer.addView(tagContainer);
        itemContainer.addView(scroll);
        resultList.add(itemContainer);
    }

    private TextView getTagView(CheckBox box, ViewGroup container) {
        TextView view = (TextView) LayoutInflater.from(context).inflate(R.layout.tag_overlay, container, false);
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop() / 2, view.getPaddingRight(), view.getPaddingBottom() / 2);
        view.setText(box.getText());
        switch (box.getId()) {
            case R.id.tag_qualification_1:
                view.setBackgroundResource(R.drawable.bg_tag_star_1);
                break;
            case R.id.tag_qualification_2:
                view.setBackgroundResource(R.drawable.bg_tag_star_2);
                break;
            case R.id.tag_qualification_5:
                view.setBackgroundResource(R.drawable.bg_tag_star_5);
                break;
            case R.id.tag_qualification_6:
                view.setBackgroundResource(R.drawable.bg_tag_star_6);
                break;
        }
        return view;
    }

    private TextView getInfoView(OperatorInfo info, ViewGroup container) {
        TextView view = (TextView) LayoutInflater.from(context).inflate(R.layout.tag_overlay, container, false);
        view.setText(info.getName());
        view.setOnClickListener(v -> {
            char space = ' ';
            StringBuilder builder = new StringBuilder();
            switch (info.getStar()) {
                case 1:
                    builder.append(context.getString(R.string.tag_qualification_1));
                    break;
//                case 2:
//                    builder.append(context.getString(R.string.tag_qualification_2));
//                    break;
                case 5:
                    builder.append(context.getString(R.string.tag_qualification_5));
                    break;
                case 6:
                    builder.append(context.getString(R.string.tag_qualification_6));
                    break;
            }
            if (builder.length() > 0)
                builder.append(space);
            builder.append(info.getType());
            for (String tag : info.getTags()) {
                builder.append(space);
                builder.append(tag);
            }
            if (context instanceof AppCompatActivity)
                Snackbar.make(container, builder.toString(), Snackbar.LENGTH_LONG).show();
            else
                Toast.makeText(context, builder.toString(), Toast.LENGTH_LONG).show();
        });
        switch (info.getStar()) {
            case 1:
                view.setBackgroundResource(R.drawable.bg_tag_star_1);
                break;
            case 2:
                view.setBackgroundResource(R.drawable.bg_tag_star_2);
                break;
            case 3:
                view.setBackgroundResource(R.drawable.bg_tag_star_3);
                break;
            case 4:
                view.setBackgroundResource(R.drawable.bg_tag_star_4);
                break;
            case 5:
                view.setBackgroundResource(R.drawable.bg_tag_star_5);
                break;
            case 6:
                view.setBackgroundResource(R.drawable.bg_tag_star_6);
                break;
        }
        return view;
    }

    private int compareInfo(OperatorInfo o1, OperatorInfo o2) {
        return manager.ascendingStar() ? o1.getStar() - o2.getStar() : o2.getStar() - o1.getStar();
    }

    private int compareTags(CheckBox t1, CheckBox t2) {
        int i;
        if (t1.getParent() == t2.getParent())
            i = ((ViewGroup) t1.getParent()).indexOfChild(t1) - ((ViewGroup) t1.getParent()).indexOfChild(t2);
        else
            i = ((ViewGroup) t1.getParent().getParent().getParent()).indexOfChild((View) t1.getParent().getParent()) - ((ViewGroup) t2.getParent().getParent().getParent()).indexOfChild((View) t2.getParent().getParent());
        return i;
    }

    private class ItemContainer extends LinearLayout implements Comparable<ItemContainer> {
        private int minStar, maxStar;

        private ItemContainer() {
            super(context);
            setOrientation(VERTICAL);
            setPadding(0, 0, 0, context.getResources().getDimensionPixelOffset(R.dimen.control_padding));
        }

        private void setStar(int min, int max) {
            minStar = min;
            maxStar = max;
        }

        private int getMaxStar() {
            return maxStar;
        }

        private int getMinStar() {
            return minStar;
        }

        @Override
        public int compareTo(@NonNull ItemContainer container) {
            int i = container.getMinStar() - minStar;
            if (i == 0) i = container.getMaxStar() - maxStar;
            return i;
        }
    }
}