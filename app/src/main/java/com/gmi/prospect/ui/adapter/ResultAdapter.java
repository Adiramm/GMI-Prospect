package com.gmi.prospect.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gmi.prospect.R;
import com.gmi.prospect.data.Course;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranked list of eligible courses on the Result screen.
 * <p>
 * Row 0 is a header ("You're eligible for N courses 🎉") so it scrolls with
 * the list; the rest are course cards. The first course gets the red
 * "⭐ Best match" badge and a blue outline. Each card has an Apply button.
 */
public class ResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Called when the user taps Apply on a course. */
    public interface OnApplyClickListener {
        void onApplyClick(@NonNull Course course);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COURSE = 1;

    private final String headline;
    private final List<Course> rankedCourses;
    private final OnApplyClickListener listener;

    /**
     * @param headline      text for the header row
     * @param rankedCourses eligible courses, best match first (from CourseRanker)
     */
    public ResultAdapter(@NonNull String headline, @NonNull List<Course> rankedCourses,
                         @NonNull OnApplyClickListener listener) {
        this.headline = headline;
        this.rankedCourses = new ArrayList<>(rankedCourses);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_COURSE;
    }

    @Override
    public int getItemCount() {
        return rankedCourses.size() + 1; // + header
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_result_header, parent, false));
        }
        return new CourseHolder(inflater.inflate(R.layout.item_result_course, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).textHeadline.setText(headline);
        } else {
            int rank = position - 1; // position 0 is the header
            ((CourseHolder) holder).bind(rankedCourses.get(rank), rank == 0, listener);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView textHeadline;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            textHeadline = itemView.findViewById(R.id.textResultHeadline);
        }
    }

    static class CourseHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View chipBestMatch;
        private final ImageView imageIcon;
        private final TextView textName;
        private final TextView textMeta;
        private final View buttonApply;

        CourseHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            chipBestMatch = itemView.findViewById(R.id.chipBestMatch);
            imageIcon = itemView.findViewById(R.id.imageCourseIcon);
            textName = itemView.findViewById(R.id.textCourseName);
            textMeta = itemView.findViewById(R.id.textCourseMeta);
            buttonApply = itemView.findViewById(R.id.buttonApply);
        }

        void bind(Course course, boolean bestMatch, OnApplyClickListener listener) {
            String name = itemView.getContext().getString(course.getNameRes());
            imageIcon.setImageResource(course.getIcon());
            textName.setText(name);
            textMeta.setText(itemView.getContext().getString(R.string.result_course_meta,
                    itemView.getContext().getString(course.getLevel().getLabelRes()),
                    itemView.getContext().getString(course.getDurationRes())));

            // Views are recycled, so always set the best-match look both ways
            chipBestMatch.setVisibility(bestMatch ? View.VISIBLE : View.GONE);
            int strokeWidth = itemView.getResources().getDimensionPixelSize(
                    bestMatch ? R.dimen.card_selected_stroke_width : R.dimen.card_stroke_width);
            int strokeColor = MaterialColors.getColor(card, bestMatch
                    ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorOutlineVariant);
            card.setStrokeWidth(strokeWidth);
            card.setStrokeColor(strokeColor);

            buttonApply.setContentDescription(
                    itemView.getContext().getString(R.string.cd_apply_for, name));
            buttonApply.setOnClickListener(v -> listener.onApplyClick(course));
        }
    }
}
