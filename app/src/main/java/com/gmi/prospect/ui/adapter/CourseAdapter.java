package com.gmi.prospect.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gmi.prospect.R;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.Faculty;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the courses of one tab in the course list.
 * <p>
 * The list can mix two kinds of rows:
 * <ul>
 *     <li>a {@link Faculty} → small section header (item_section_header.xml)</li>
 *     <li>a {@link Course} → course card (item_course.xml)</li>
 * </ul>
 * so the Diploma tab can be grouped by faculty while GAPP / GUFP show cards only.
 */
public class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Called when the user taps a course card. */
    public interface OnCourseClickListener {
        void onCourseClick(@NonNull Course course);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COURSE = 1;

    /** Each item is either a Faculty (header) or a Course (card). */
    private final List<Object> items = new ArrayList<>();
    private final OnCourseClickListener listener;

    public CourseAdapter(@NonNull OnCourseClickListener listener) {
        this.listener = listener;
    }

    /** Replaces the whole list (called when the user switches tab). */
    @SuppressLint("NotifyDataSetChanged") // every row changes on a tab switch
    public void setItems(@NonNull List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Faculty ? TYPE_HEADER : TYPE_COURSE;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_section_header, parent, false));
        }
        return new CourseHolder(inflater.inflate(R.layout.item_course, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind((Faculty) item);
        } else {
            ((CourseHolder) holder).bind((Course) item, listener);
        }
    }

    /** Section header, e.g. "ELECTRICAL ENGINEERING". */
    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textSectionHeader);
        }

        void bind(Faculty faculty) {
            textTitle.setText(faculty.getLabelRes());
        }
    }

    /** Course card: icon, name, duration and an optional "Limited seats" chip. */
    static class CourseHolder extends RecyclerView.ViewHolder {
        private final ImageView imageIcon;
        private final TextView textName;
        private final TextView textDuration;
        private final View chipLimitedSeats;

        CourseHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.imageCourseIcon);
            textName = itemView.findViewById(R.id.textCourseName);
            textDuration = itemView.findViewById(R.id.textCourseDuration);
            chipLimitedSeats = itemView.findViewById(R.id.chipLimitedSeats);
        }

        void bind(Course course, OnCourseClickListener listener) {
            imageIcon.setImageResource(course.getIcon());
            textName.setText(course.getNameRes());
            textDuration.setText(course.getDurationRes());
            // Views are recycled, so always set visibility both ways
            chipLimitedSeats.setVisibility(course.isLimitedSeats() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> listener.onCourseClick(course));
        }
    }
}
