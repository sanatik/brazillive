package com.poker.brazillive.shell.util;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

/**
 * �����-������� ��� ������������ ��� java ���������� ������ � �����������
 * ����������� ������ ������� Pattern, Matcher � ������ ������������ ������� �
 * ������ �� ������������� ����������� php
 */
public class Re {

    /**
     * ��������� ������� ������ ����������� ��� ��� ����� ��������� ���������,
     * ������ ������� ��������� ����������
     */
    public static interface Replacer {

        /**
         * ����� ������ ������� ������ �� ������� ����� ��������� ������
         * ���������� regexp-�� ���������
         *
         * @param matches ������ � ����������� �� ��������� ���������, �������
         * ������� ������ �������� ���� ����� "����������" ��������� �� ��������
         * 1,2, ... �������� �������� ��� ����� ������ ����������� ���������
         * @return
         */
        public String onMatch(List<String> matches);
    }

    /**
     * ���, � ������� �������� ���������������� regexp-���������
     */
    private static Map<String, Pattern> cache = new ConcurrentHashMap<String, Pattern>();

    /**
     * ������ ���� ���������������� regexp-���������
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * ��������� ����� � ������ ������� � �������� ��� �� ����� ��������
     * ����������� �����������, �������������
     *
     * @param pattern ������ (regexp)
     * @param input ������, ��� ��������� �����
     * @param by ������ Replacer - ������ �������� �� ��� ��������� ������
     * @return ������ ����� ������
     */
    public static String preg_replace_callback(String pattern, String input, Replacer by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        StringBuffer sb = new StringBuffer();
        ArrayList<String> row = new ArrayList<String>();

        while (m.find()) {
            try {
                row.clear();
                for (int i = 0; i <= gcount; i++) {
                    row.add(m.group(i));
                }
                m.appendReplacement(sb, by.onMatch(row));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * ��������� ����� � ������ ������� � �������� ��� �� ����� ��������
     * ����������� ���������� Regexp-���������
     *
     * @param pattern ������ (regexp)
     * @param input ������, ��� ��������� �����
     * @param by ������, �� ������� ����� �������� ��������� ��������
     * @return ������ ����� ������
     */
    public static String preg_replace(String pattern, String input, String by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                m.appendReplacement(sb, by);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * �������� ���� ������������ �� ������ � ��������
     *
     * @param pattern ������ (regexp)
     * @param input ������, ��� ��������� �����
     * @param rez ������ ���� ����� �������� ���������� �� ����������: �������
     * ������� ������ �������� ���� ����� ���������� 1, 2, ... �������� ��������
     * �����
     * @return ������ ��������� - ������� ���� ��� ���������� ���������
     */
    public static boolean preg_match(String pattern, String input, List<String> rez) {
        Pattern p = compile(pattern, true);
        return preg_match(p, input, rez);
    }

    public static boolean preg_match(Pattern p, String input, List<String> rez) {
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        if (rez != null) {
            rez.clear();
        }
        if (m.matches()) {
            for (int i = 0; i <= gcount; i++) {
                if (rez != null) {
                    rez.add(m.group(i));
                }
            }
        }
        return rez.size() > 0;
    }

    /**
     * �������� ���� ��� � ������ ���������� ��������� ������ � ������������
     * ������ �� ����� ���������� �������� ����������
     *
     * @param pattern ������ (regexp)
     * @param input ������, ��� ��������� �����
     * @param rez ������, ���� ����� �������� ��� ��������� ����������, ������
     * �������������: ������ ������� �������� ������������ ��������-�������,
     * ������ �� ������� �������� ���������� �� ��������� ���������� � ����� ��
     * ������� ��� � ����� preg_match
     * @return
     */
    public static boolean preg_match_all(String pattern, String input, List<List<String>> rez) {
        Pattern p = compile(pattern, true);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        if (rez != null) {
            rez.clear();
        }
        while (m.find()) {
            ArrayList row = new ArrayList();
            for (int i = 0; i <= gcount; i++) {
                if (rez != null) {
                    row.add(m.group(i));
                }
            }
            if (rez != null) {
                rez.add(row);
            }
        }
        return rez.size() > 0;
    }

    /**
     * ��������� ����� ����������� ���������� regexp-� � ���������� ��� � ���
     *
     * @param pattern ����� ����������� ���������
     * @param surroundBy ������� ���� ����� �� ��������� �������� .*?
     * @return ���������������� Pattern
     */
    public static Pattern compile(String pattern, boolean surroundBy) {
        synchronized (cache) {
            if (cache.containsKey(pattern)) {
                return cache.get(pattern);
            }
        }
        final String pattern_orig = pattern;

        final char firstChar = pattern.charAt(0);
        char endChar = firstChar;
        if (firstChar == '(') {
            endChar = '}';
        }
        if (firstChar == '[') {
            endChar = ']';
        }
        if (firstChar == '{') {
            endChar = '}';
        }
        if (firstChar == '<') {
            endChar = '>';
        }

        int lastPos = pattern.lastIndexOf(endChar);
        if (lastPos == -1) {
            throw new RuntimeException("Invalid pattern: " + pattern);
        }

        char[] modifiers = pattern.substring(lastPos + 1).toCharArray();
        int mod = 0;
        for (int i = 0; i < modifiers.length; i++) {
            char modifier = modifiers[i];
            switch (modifier) {
                case 'i':
                    mod |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'd':
                    mod |= Pattern.UNIX_LINES;
                    break;
                case 'x':
                    mod |= Pattern.COMMENTS;
                    break;
                case 'm':
                    mod |= Pattern.MULTILINE;
                    break;
                case 's':
                    mod |= Pattern.DOTALL;
                    break;
                case 'u':
                    mod |= Pattern.UNICODE_CASE;
                    break;
            }
        }
        pattern = pattern.substring(1, lastPos);
        if (surroundBy) {
            if (pattern.charAt(0) != '^') {
                pattern = ".*?" + pattern;
            }
            if (pattern.charAt(pattern.length() - 1) != '$') {
                pattern = pattern + ".*?";
            }
        }

        final Pattern rezPattern = Pattern.compile(pattern, mod);
        cache.put(pattern_orig, rezPattern);
        return rezPattern;
    }
}
