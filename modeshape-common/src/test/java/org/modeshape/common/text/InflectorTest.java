/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.common.text;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class InflectorTest {

    private Inflector inflector;

    @Before
    public void beforeEach() {
        this.inflector = new Inflector();
    }

    public void singularToPlural( Object singular,
                                  String expectedPlural ) {
        // Test pluralizing the singular string
        String actualPlural = inflector.pluralize(singular);
        assertEquals(expectedPlural, actualPlural);

        // Test singularizing the given (expected) pluralized form
        String actualSingular = inflector.singularize(expectedPlural);
        assertEquals(singular, actualSingular);

        // Test singularizing the already singular form (should not change)
        assertEquals(singular, inflector.singularize(singular));

        // Test pluralizing the already plural form (should not change)
        assertEquals(expectedPlural, inflector.pluralize(expectedPlural));
    }

    public void upperCamelCase( String word,
                                String expectedCamelized,
                                char... delimiterChars ) {
        // Test uppercasing the string
        String actualCamelized = inflector.camelCase(word, true, delimiterChars);
        assertEquals(expectedCamelized, actualCamelized);
        assertEquals(expectedCamelized, inflector.upperCamelCase(word, delimiterChars));

        if (delimiterChars == null || delimiterChars.length == 0) {
            // Test underscoring the camelized word ...
            String actualUnderscored = inflector.underscore(expectedCamelized);
            assertEquals(word, actualUnderscored);
        }
    }

    public void lowerCamelCase( String word,
                                String expectedCamelized,
                                char... delimiterChars ) {
        // Test lowercasing the string
        String actualCamelized = inflector.camelCase(word, false, delimiterChars);
        assertEquals(expectedCamelized, actualCamelized);
        assertEquals(expectedCamelized, inflector.lowerCamelCase(word, delimiterChars));

        if (delimiterChars == null || delimiterChars.length == 0) {
            // Test underscoring the camelized word ...
            String actualUnderscored = inflector.underscore(expectedCamelized);
            assertEquals(word, actualUnderscored);
        }
    }

    public void underscore( String word,
                            String expectedUnderscored,
                            char... delimiterChars ) {
        // Test underscoring the word
        String actualUnderscored = inflector.underscore(word, delimiterChars);
        assertEquals(expectedUnderscored, actualUnderscored);
    }

    public void capitalize( String words,
                            String expectedValue ) {
        // Test capitalizing the phrase
        String actualValue = inflector.capitalize(words);
        assertEquals(expectedValue, actualValue);
    }

    public void humanize( String word,
                          String expectedValue,
                          String... removableTokens ) {
        // Test humanizing the word
        String actualValue = inflector.humanize(word, removableTokens);
        assertEquals(expectedValue, actualValue);
    }

    public void titleCase( String word,
                           String expectedValue,
                           String... removableTokens ) {
        // Test title casing the word
        String actualValue = inflector.titleCase(word, removableTokens);
        assertEquals(expectedValue, actualValue);
    }

    public void ordinalize( int number,
                            String expectedValue ) {
        // Test underscoring the camelized word
        String actualValue = inflector.ordinalize(number);
        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void shouldReplaceAllWithUppercase() {
        assertEquals("hEllO", Inflector.replaceAllWithUppercase("hello", "([aeiou])", 1));
        assertEquals("hLlo", Inflector.replaceAllWithUppercase("hello", "([aeiou])(l)", 2));
    }

    @Test
    public void shouldPluralizeAndSingularize() {
        // These are examples of words that do not have special rules
        singularToPlural("class", "classes");
        singularToPlural("glass", "glasses");
        singularToPlural("package", "packages");
        singularToPlural("setting", "settings");
        singularToPlural("sample", "samples");
        singularToPlural("message", "messages");
        singularToPlural("content", "contents");
        singularToPlural("ball", "balls");
        // These are special cases that are handled by standard rules ...
        singularToPlural("axis", "axes");
        singularToPlural("octopus", "octopi");
        singularToPlural("virus", "viri");
        singularToPlural("alien", "aliens");
        singularToPlural("status", "statuses");
        singularToPlural("bus", "buses");
        singularToPlural("buffalo", "buffaloes");
        singularToPlural("tomato", "tomatoes");

        singularToPlural("quiz", "quizzes");
        singularToPlural("party", "parties");
        singularToPlural("half", "halves");
        singularToPlural("stadium", "stadiums");

        // From activesupport/test/inflector_test.rb
        singularToPlural("search", "searches");
        singularToPlural("switch", "switches");
        singularToPlural("fix", "fixes");
        singularToPlural("box", "boxes");
        singularToPlural("process", "processes");
        singularToPlural("address", "addresses");
        singularToPlural("case", "cases");
        singularToPlural("stack", "stacks");
        singularToPlural("wish", "wishes");
        singularToPlural("fish", "fish");

        singularToPlural("category", "categories");
        singularToPlural("query", "queries");
        singularToPlural("ability", "abilities");
        singularToPlural("agency", "agencies");
        singularToPlural("movie", "movies");

        singularToPlural("archive", "archives");

        singularToPlural("index", "indices");

        singularToPlural("wife", "wives");
        singularToPlural("safe", "saves");
        singularToPlural("half", "halves");

        singularToPlural("move", "moves");

        singularToPlural("salesperson", "salespeople");
        singularToPlural("person", "people");

        singularToPlural("spokesman", "spokesmen");
        singularToPlural("man", "men");
        singularToPlural("woman", "women");

        singularToPlural("basis", "bases");
        singularToPlural("diagnosis", "diagnoses");

        singularToPlural("datum", "data");
        singularToPlural("medium", "media");
        singularToPlural("analysis", "analyses");

        singularToPlural("node_child", "node_children");
        singularToPlural("child", "children");

        singularToPlural("experience", "experiences");
        singularToPlural("day", "days");

        singularToPlural("comment", "comments");
        singularToPlural("foobar", "foobars");
        singularToPlural("newsletter", "newsletters");

        singularToPlural("old_news", "old_news");
        singularToPlural("news", "news");

        singularToPlural("series", "series");
        singularToPlural("species", "species");

        singularToPlural("quiz", "quizzes");

        singularToPlural("perspective", "perspectives");

        singularToPlural("ox", "oxen");
        singularToPlural("photo", "photos");
        singularToPlural("buffalo", "buffaloes");
        singularToPlural("tomato", "tomatoes");
        singularToPlural("dwarf", "dwarves");
        singularToPlural("elf", "elves");
        singularToPlural("information", "information");
        singularToPlural("equipment", "equipment");
        singularToPlural("bus", "buses");
        singularToPlural("status", "statuses");
        singularToPlural("status_code", "status_codes");
        singularToPlural("mouse", "mice");

        singularToPlural("louse", "lice");
        singularToPlural("house", "houses");
        singularToPlural("octopus", "octopi");
        singularToPlural("virus", "viri");
        singularToPlural("alias", "aliases");
        singularToPlural("portfolio", "portfolios");

        singularToPlural("vertex", "vertices");
        singularToPlural("matrix", "matrices");

        singularToPlural("axis", "axes");
        singularToPlural("testis", "testes");
        singularToPlural("crisis", "crises");

        singularToPlural("rice", "rice");
        singularToPlural("shoe", "shoes");

        singularToPlural("horse", "horses");
        singularToPlural("prize", "prizes");
        singularToPlural("edge", "edges");
    }

    @Test
    public void shouldConvertToCamelCase() {
        lowerCamelCase("edge", "edge");
        lowerCamelCase("active_record", "activeRecord");
        lowerCamelCase("product", "product");
        lowerCamelCase("special_guest", "specialGuest");
        lowerCamelCase("application_controller", "applicationController");
        lowerCamelCase("area51_controller", "area51Controller");
        lowerCamelCase("the-first_name", "theFirstName", '-');

        upperCamelCase("edge", "Edge");
        upperCamelCase("active_record", "ActiveRecord");
        upperCamelCase("product", "Product");
        upperCamelCase("special_guest", "SpecialGuest");
        upperCamelCase("application_controller", "ApplicationController");
        upperCamelCase("area51_controller", "Area51Controller");
        upperCamelCase("the-first_name", "TheFirstName", '-');
    }

    @Test
    public void shouldConvertToUnderscore() {
        underscore("activeRecord", "active_record");
        underscore("ActiveRecord", "active_record");
        underscore("ACTIVERecord", "active_record");
        underscore("firstName", "first_name");
        underscore("FirstName", "first_name");
        underscore("name", "name");
        underscore("The.firstName", "the_first_name", '.');

    }

    @Test
    public void shouldCapitalize() {
        capitalize("active record", "Active record");
        capitalize("first name", "First name");
        capitalize("name", "Name");
        capitalize("the first name", "The first name");
        capitalize("employee_salary", "Employee_salary");
        capitalize("underground", "Underground");
    }

    @Test
    public void shouldHumanize() {
        humanize("active_record", "Active record");
        humanize("first_name", "First name");
        humanize("name", "Name");
        humanize("the_first_name", "The first name");
        humanize("employee_salary", "Employee salary");
        humanize("underground", "Underground");
        humanize("id", "Id");
        humanize("employee_id", "Employee");
        humanize("employee_value_string", "Employee string", "value");
    }

    @Test
    public void shouldConvertToTitleCase() {
        titleCase("active_record", "Active Record");
        titleCase("first_name", "First Name");
        titleCase("name", "Name");
        titleCase("the_first_name", "The First Name");
        titleCase("employee_salary", "Employee Salary");
        titleCase("underground", "Underground");
        titleCase("id", "Id");
        titleCase("employee_id", "Employee");
        titleCase("employee_value_string", "Employee String", "value");
    }

    @Test
    public void shouldOrdinalize() {
        ordinalize(1, "1st");
        ordinalize(2, "2nd");
        ordinalize(3, "3rd");
        ordinalize(4, "4th");
        ordinalize(5, "5th");
        ordinalize(6, "6th");
        ordinalize(7, "7th");
        ordinalize(8, "8th");
        ordinalize(9, "9th");
        ordinalize(10, "10th");
        ordinalize(11, "11th");
        ordinalize(12, "12th");
        ordinalize(13, "13th");
        ordinalize(14, "14th");
        ordinalize(15, "15th");
        ordinalize(16, "16th");
        ordinalize(17, "17th");
        ordinalize(18, "18th");
        ordinalize(19, "19th");
        ordinalize(20, "20th");
        ordinalize(21, "21st");
        ordinalize(22, "22nd");
        ordinalize(23, "23rd");
        ordinalize(24, "24th");
        ordinalize(25, "25th");
        ordinalize(26, "26th");
        ordinalize(27, "27th");
        ordinalize(28, "28th");
        ordinalize(29, "29th");
        ordinalize(30, "30th");
        ordinalize(31, "31st");
        ordinalize(32, "32nd");
        ordinalize(33, "33rd");
        ordinalize(34, "34th");
        ordinalize(35, "35th");
        ordinalize(36, "36th");
        ordinalize(37, "37th");
        ordinalize(38, "38th");
        ordinalize(39, "39th");
        ordinalize(100, "100th");
        ordinalize(101, "101st");
        ordinalize(102, "102nd");
        ordinalize(103, "103rd");
        ordinalize(104, "104th");
        ordinalize(200, "200th");
        ordinalize(201, "201st");
        ordinalize(202, "202nd");
        ordinalize(203, "203rd");
        ordinalize(204, "204th");
        ordinalize(1000, "1000th");
        ordinalize(1001, "1001st");
        ordinalize(1002, "1002nd");
        ordinalize(1003, "1003rd");
        ordinalize(1004, "1004th");
        ordinalize(10000, "10000th");
        ordinalize(10001, "10001st");
        ordinalize(10002, "10002nd");
        ordinalize(10003, "10003rd");
        ordinalize(10004, "10004th");
        ordinalize(100000, "100000th");
        ordinalize(100001, "100001st");
        ordinalize(100002, "100002nd");
        ordinalize(100003, "100003rd");
        ordinalize(100004, "100004th");
    }

}
