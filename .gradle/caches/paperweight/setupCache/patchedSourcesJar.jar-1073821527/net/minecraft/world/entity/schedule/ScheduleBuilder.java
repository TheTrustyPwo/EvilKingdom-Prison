package net.minecraft.world.entity.schedule;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;

public class ScheduleBuilder {
    private final Schedule schedule;
    private final List<ScheduleBuilder.ActivityTransition> transitions = Lists.newArrayList();

    public ScheduleBuilder(Schedule schedule) {
        this.schedule = schedule;
    }

    public ScheduleBuilder changeActivityAt(int startTime, Activity activity) {
        this.transitions.add(new ScheduleBuilder.ActivityTransition(startTime, activity));
        return this;
    }

    public Schedule build() {
        this.transitions.stream().map(ScheduleBuilder.ActivityTransition::getActivity).collect(Collectors.toSet()).forEach(this.schedule::ensureTimelineExistsFor);
        this.transitions.forEach((activity) -> {
            Activity activity2 = activity.getActivity();
            this.schedule.getAllTimelinesExceptFor(activity2).forEach((timeline) -> {
                timeline.addKeyframe(activity.getTime(), 0.0F);
            });
            this.schedule.getTimelineFor(activity2).addKeyframe(activity.getTime(), 1.0F);
        });
        return this.schedule;
    }

    static class ActivityTransition {
        private final int time;
        private final Activity activity;

        public ActivityTransition(int startTime, Activity activity) {
            this.time = startTime;
            this.activity = activity;
        }

        public int getTime() {
            return this.time;
        }

        public Activity getActivity() {
            return this.activity;
        }
    }
}
