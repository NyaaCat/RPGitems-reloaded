package think.rpgitems.commands;

import think.rpgitems.power.TriggerType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PowerMeta {
    boolean immutableTrigger() default false;

    TriggerType[] defaultTrigger() default {};

    boolean marker() default false;
}
