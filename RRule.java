package cn.duckr.util;


import cn.duckr.model.EventBean;

/**
 * Created by GG on 2016/12/28.
 */

public class RRule {
    private EventBean eventBean;

    StringBuffer rRule = new StringBuffer();
    StringBuffer weekType = new StringBuffer();

    public RRule(EventBean eventBean) {
        this.eventBean = eventBean;
        init();
    }

    public void init() {
        int weekSK = ComFuncs.getWeek(eventBean.getStartTime());

        int days = Integer.parseInt(ComFuncs.getTwoDay(eventBean.getEndTime(), eventBean.getStartTime()));

        for (int i = 0; i <= days; i++) {
            setWeek(weekSK + i);
        }
        String until = eventBean.getEndRepeatDate().replace("-", "");
        switch (eventBean.getRepeatType()) {//设置重复属性
            case 0://永不 添加终止时间

                break;
            case 1://每天
                rRule.append("FREQ=DAILY;");
                break;
            case 2://每周
                rRule.append("FREQ=WEEKLY;");
                break;
            case 3://每两周
                rRule.append("FREQ=WEEKLY;INTERVAL=2;");
                break;
            case 4://每月
                rRule.append("FREQ=MONTHLY;");
                break;
            case 5://每年
                rRule.append("FREQ=YEARLY;");
                break;
        }
        rRule.append("UNTIL=").append(until).append("T000000Z;");
        rRule.append("WKST=SU");
        if (eventBean.getRepeatType() != 1)//每天发生的 不设置按星期添加事件
            rRule.append(days < 7 ? ";BYDAY=" + weekType.toString().substring(0,weekType.length()-1) : "");
    }

    public String getRRule() {
        return rRule.toString();
    }

    private void setWeek(int i) {
        if (i > 7)
            setWeek(i - 7);
        else
            switch (i) {
                case 1:
                    weekType.append("SU,");
                    break;
                case 2:
                    weekType.append("MO,");
                    break;
                case 3:
                    weekType.append("TU,");
                    break;
                case 4:
                    weekType.append("WE,");
                    break;
                case 5:
                    weekType.append("TH,");
                    break;
                case 6:
                    weekType.append("FR,");
                    break;
                case 7:
                    weekType.append("SA,");
                    break;
            }
    }










     /**
     * 检查是否有现有存在的账户。存在则返回账户id，否则返回-1
     */
    private static int checkCalendarAccount(Context context) {
        Cursor userCursor = context.getContentResolver().query(Uri.parse(CALENDER_URL), null, null, null, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " ASC ");//升序排序
        try {
            if (userCursor == null)//查询返回空值
                return -1;
            int count = userCursor.getCount();
            if (count > 0) {//存在现有账户，取第一个账户的id返回

                userCursor.moveToLast();//获取到最高权限的日历账户
                return userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID));
            } else {
                return -1;
            }
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }
    }

    /**
     * 添加账户。账户创建成功则返回账户id，否则返回-1
     */
    private static long addCalendarAccount(Context context) {
        TimeZone timeZone = TimeZone.getDefault();
        ContentValues value = new ContentValues();
        value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);

        value.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE);
        value.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
        value.put(CalendarContract.Calendars.VISIBLE, 1);
        value.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.BLUE);
        value.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        value.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID());
        value.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

        Uri calendarUri = Uri.parse(CALENDER_URL);
        calendarUri = calendarUri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE)
                .build();

        Uri result = context.getContentResolver().insert(calendarUri, value);
        long id = result == null ? -1 : ContentUris.parseId(result);
        return id;
    }

    /**
     * 获取账户。如果账户不存在则先创建账户，账户存在获取账户id；获取账户成功返回账户id，否则返回-1
     */
    private static int checkAndAddCalendarAccount(Context context) {
        int oldId = checkCalendarAccount(context);
        if (oldId >= 0) {
            return oldId;
        } else {
            long addId = addCalendarAccount(context);
            if (addId >= 0) {
                return checkCalendarAccount(context);
            } else {
                return -1;
            }
        }
    }

    /**
     * 添加日历事件、日程
     */
    public static EventBean addCalendarEvent(Context context, EventBean eventBean) {
        // 获取日历账户的id
        int calId = checkAndAddCalendarAccount(context);
        if (calId < 0) {
            // 获取账户id失败直接返回，添加日历事件失败
            return null;
        }

        ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, eventBean.getTitle());
        if (eventBean.getBrief() != null)
            event.put(CalendarContract.Events.DESCRIPTION, eventBean.getBrief());
        // 插入账户的id
        if (eventBean.getPlaceName() != null)
            event.put(CalendarContract.Events.EVENT_LOCATION, eventBean.getPlaceName());

        event.put(CalendarContract.Events.CALENDAR_ID, calId);

        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(eventBean.getIsAllDay() == 0 ? str2LongWHour(eventBean.getStartTime()) : str2LongWHour(eventBean.getStartTime()) + 24 * 60 * 60 * 1000);//设置开始时间
        long start = mCalendar.getTime().getTime();
        event.put(CalendarContract.Events.DTSTART, start);
        //日历的重复设置
        if (eventBean.getRepeatType() == 0) {//永不 添加终止时间
            mCalendar.setTimeInMillis(str2LongWHour(eventBean.getEndTime()));//设置终止时间
            long end = mCalendar.getTime().getTime();
            event.put(CalendarContract.Events.DTEND, end);
        } else {
            long duration = str2LongWHour(eventBean.getEndTime()) - str2LongWHour(eventBean.getStartTime());
            event.put(CalendarContract.Events.DURATION, "PT" + duration / 1000 / 60 + "M");

            RRule rRule = new RRule(eventBean);
            event.put(CalendarContract.Events.RRULE, rRule.getRRule());
        }

        event.put(CalendarContract.Events.HAS_ALARM, 1);//设置有闹钟提醒

        event.put(CalendarContract.Events.ALL_DAY, eventBean.getIsAllDay());
        TimeZone tz = TimeZone.getDefault();
        event.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getID());  //这个是时区，必须有，

        //添加事件
        Uri newEvent = context.getContentResolver().insert(Uri.parse(CALENDER_EVENT_URL), event);
        long eventId = Long.parseLong(newEvent != null ? newEvent.getLastPathSegment() : null);
        LogHelper.w("eventId", eventId + "");
        eventBean.setEventIdentifier(eventId + "");
        if (newEvent == null) {
            // 添加日历事件失败直接返回
            return null;
        }
        //事件提醒的设定
        setCalendarRemind(context, newEvent, eventBean.getRemindType());
        return eventBean;
    }


    private static void setCalendarRemind(Context context, Uri newEvent, int remindType) {
        if (remindType == 0)
            return;
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(newEvent));
        if (remindType == 1)
            values.put(CalendarContract.Reminders.MINUTES, 15);
        else if (remindType == 2)
            values.put(CalendarContract.Reminders.MINUTES, 30);
        else if (remindType == 3)
            values.put(CalendarContract.Reminders.MINUTES, 60);
        else if (remindType == 4)
            values.put(CalendarContract.Reminders.MINUTES, 2 * 60);
        else if (remindType == 5)
            values.put(CalendarContract.Reminders.MINUTES, 24 * 60);
        else if (remindType == 6)
            values.put(CalendarContract.Reminders.MINUTES, 2 * 24 * 60);
        else if (remindType == 7)
            values.put(CalendarContract.Reminders.MINUTES, 7 * 24 * 60);
        else if (remindType == 9)
            values.put(CalendarContract.Reminders.MINUTES, 30 * 24 * 60);
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        Uri uri = context.getContentResolver().insert(Uri.parse(CALENDER_REMINDER_URL), values);
        if (uri == null) {
            // 添加闹钟提醒失败直接返回
        }
    }


    /**
     * 根据设置的title来查找并删除
     */
    public static void deleteCalendarEvent(Context context, String title) {
        Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALENDER_EVENT_URL), null, null, null, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " ASC ");
        try {
            if (eventCursor == null)//查询返回空值
                return;
            if (eventCursor.getCount() > 0) {
                //遍历所有事件，找到title跟需要查询的title一样的项
                for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
                    String eventTitle = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.TITLE));
                    if (!TextUtils.isEmpty(title) && title.equals(eventTitle)) {
                        int id = eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Calendars._ID));//取得id
                        Uri deleteUri = ContentUris.withAppendedId(Uri.parse(CALENDER_EVENT_URL), id);
                        int rows = context.getContentResolver().delete(deleteUri, null, null);
                        if (rows == -1) {
                            //事件删除失败
                            return;
                        }
                    }
                }
            }
        } finally {
            if (eventCursor != null) {
                eventCursor.close();
            }
        }
    }

    public static void getCalendarEvent(Context context) {
        Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALENDER_EVENT_URL), null, null, null, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " ASC ");
//        if (eventCursor.getCount() > 0) {
//            eventCursor.moveToLast();             //注意：这里与添加事件时的账户相对应，都是向最后一个账户添加
//            String eventTitle = eventCursor.getString(eventCursor.getColumnIndex("title"));
//            Toast.makeText(MainActivity.this, eventTitle, Toast.LENGTH_LONG).show();
//        }
        try {
            if (eventCursor == null)//查询返回空值
                return;
            if (eventCursor.getCount() > 0) {
                //遍历所有事件，找到title跟需要查询的title一样的项
                for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
                    String eventTitle = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.TITLE));
                    String eventLocaltion = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION));
                    String eventStartTime = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.DTSTART));
                    String eventEndTime = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.DTEND));
                    String eventDescription = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                    String eventDuration = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.DURATION));
                    String eventRule = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.RRULE));
                    String eventDate = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.RDATE));

                    LogHelper.w("eventTitle", eventTitle);
                    LogHelper.w("eventLocaltion", eventLocaltion);
                    LogHelper.w("eventStartTime", eventStartTime);
                    LogHelper.w("eventEndTime", eventEndTime);
                    LogHelper.w("eventDescription", eventDescription);
                    LogHelper.w("eventDuration", eventDuration);
                    LogHelper.w("eventRule", eventRule);
                    LogHelper.w("eventDate", eventDate);
//                    if (!TextUtils.isEmpty(title) && title.equals(eventTitle)) {
//                        int id = eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Calendars._ID));//取得id
//                        Uri deleteUri = ContentUris.withAppendedId(Uri.parse(CALENDER_EVENT_URL), id);
//                        int rows = context.getContentResolver().delete(deleteUri, null, null);
//                        if (rows == -1) {
//                            //事件删除失败
//                            return;
//                        }
//                    }
                }
            }
        } finally {
            if (eventCursor != null) {
                eventCursor.close();
            }
        }

    }





public class EventBean {
    @SerializedName("Euid")
    private String Euid;
    @SerializedName("Title")
    private String Title;
    @SerializedName("PlaceName")
    private String PlaceName;
    @SerializedName("StartTime")
    private String StartTime;
    @SerializedName("EndTime")
    private String EndTime;
    @SerializedName("RepeatType")
    private int RepeatType;
    @SerializedName("ThemeId")
    private int ThemeId;
    @SerializedName("RelateUser")
    private String RelateUser;
    @SerializedName("RemindType")
    private int RemindType;
    @SerializedName("RelateUrl")
    private String RelateUrl;
    @SerializedName("Brief")
    private String Brief;
    @SerializedName("ActivAuid")
    private String ActivAuid;
    @SerializedName("IsAllDay")
    private int IsAllDay;
    @SerializedName("EndRepeatDate")
    private String EndRepeatDate;
    @SerializedName("EventIdentifier")
    private String EventIdentifier;

    public String getEuid() {
        return Euid;
    }

    public void setEuid(String euid) {
        Euid = euid;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getPlaceName() {
        return PlaceName;
    }

    public void setPlaceName(String placeName) {
        PlaceName = placeName;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public int getRepeatType() {
        return RepeatType;
    }

    public void setRepeatType(int repeatType) {
        RepeatType = repeatType;
    }

    public int getThemeId() {
        return ThemeId;
    }

    public void setThemeId(int themeId) {
        ThemeId = themeId;
    }

    public String getRelateUser() {
        return RelateUser;
    }

    public void setRelateUser(String relateUser) {
        RelateUser = relateUser;
    }

    public int getRemindType() {
        return RemindType;
    }

    public void setRemindType(int remindType) {
        RemindType = remindType;
    }

    public String getRelateUrl() {
        return RelateUrl;
    }

    public void setRelateUrl(String relateUrl) {
        RelateUrl = relateUrl;
    }

    public String getBrief() {
        return Brief;
    }

    public void setBrief(String brief) {
        Brief = brief;
    }

    public String getActivAuid() {
        return ActivAuid;
    }

    public void setActivAuid(String activAuid) {
        ActivAuid = activAuid;
    }

    public int getIsAllDay() {
        return IsAllDay;
    }

    public void setIsAllDay(int isAllDay) {
        IsAllDay = isAllDay;
    }

    public String getEndRepeatDate() {
        return EndRepeatDate;
    }

    public void setEndRepeatDate(String endRepeatDate) {
        EndRepeatDate = endRepeatDate;
    }

    public String getEventIdentifier() {
        return EventIdentifier;
    }

    public void setEventIdentifier(String eventIdentifier) {
        EventIdentifier = eventIdentifier;
    }
}
    
}
