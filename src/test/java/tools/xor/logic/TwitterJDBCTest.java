/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package tools.xor.logic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tools.xor.service.AggregateManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class TwitterJDBCTest
{
    @Autowired
    protected AggregateManager am;

    @Autowired
    protected DataSource dataSource;

    @Before
    public void init() throws SQLException, ClassNotFoundException, IOException
    {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {

            statement.execute("CREATE TABLE user "
                    + "(id INTEGER NOT NULL, "
                    + " ID_STR VARCHAR(20) NOT NULL, "
                    + " name VARCHAR(50) NOT NULL, "
                    + " screen_name VARCHAR(15) NOT NULL, "
                    + " PRIMARY KEY(id_str))");

            // Placeholder to represent an object of inverse collection relationships
            statement.execute("CREATE TABLE entities_container "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");

            statement.execute("CREATE TABLE tweet "
                    + "(created_at DATE NOT NULL, "
                    + " id INTEGER NOT NULL, "
                    + " ID_STR VARCHAR(20) NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " source VARCHAR(128) NOT NULL, "
                    + " truncated BOOLEAN NOT NULL, "
                    + " in_reply_to_status_id INTEGER, "
                    + " in_reply_to_status_id_str VARCHAR(20), "
                    + " in_reply_to_user_id INTEGER, "
                    + " in_reply_to_user_id_str VARCHAR(20), "
                    + " in_reply_to_screen_name VARCHAR(30), "
                    + " user VARCHAR(20) NOT NULL, "
                    + " coordinates VARCHAR(20), "
                    + " place VARCHAR(20), "
                    + " is_quote_status BOOLEAN NOT NULL, "
                    + " quote_count INTEGER, "
                    + " reply_count INTEGER NOT NULL, "
                    + " retweet_count INTEGER NOT NULL, "
                    + " favorite_count INTEGER, "
                    + " entities VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE tweet ADD CONSTRAINT FK1_1__1_tweet FOREIGN KEY(entities) REFERENCES entities_container(id_str)");
            statement.execute("ALTER TABLE tweet ADD CONSTRAINT FK2_1__1_user FOREIGN KEY(user) REFERENCES user(id_str)");

            // Inherits from tweet table
            statement.execute("CREATE TABLE quotetweet "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " quoted_status_id INTEGER NOT NULL, "
                    + " quoted_status_id_str VARCHAR(20) NOT NULL, "
                    + " quoted_status VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE quotetweet ADD CONSTRAINT FK1_1__1_quotetweet FOREIGN KEY(id_str) REFERENCES tweet(id_str)");
            statement.execute("ALTER TABLE quotetweet ADD CONSTRAINT FK2_1__N_quotetweets FOREIGN KEY(quoted_status) REFERENCES tweet(id_str)");

            // Inherits from tweet table
            statement.execute("CREATE TABLE retweettweet "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " retweeted_status VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE retweettweet ADD CONSTRAINT FK1_1__1_retweettweet FOREIGN KEY(id_str) REFERENCES tweet(id_str)");
            statement.execute("ALTER TABLE retweettweet ADD CONSTRAINT FK2_1__N_retweets FOREIGN KEY(retweeted_status) REFERENCES tweet(id_str)");

            statement.execute("CREATE TABLE indices "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " entity_id_str VARCHAR(20) NOT NULL, "
                    + " value INTEGER, "
                    + " position INTEGER, "
                    + " PRIMARY KEY(id_str))");

            statement.execute("CREATE TABLE hashtags "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE hashtags ADD CONSTRAINT FK1_1__N_hashtags FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE urls "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " url VARCHAR(1024) NOT NULL, "
                    + " display_url VARCHAR(1024) NOT NULL, "
                    + " expanded_url VARCHAR(1024) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE urls ADD CONSTRAINT FK1_1__N_urls FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE user_mentions "
                    + "(UM_ID_STR VARCHAR(20) NOT NULL, "
                    + " name VARCHAR(280) NOT NULL, "
                    + " id INTEGER NOT NULL, "
                    + " id_str VARCHAR(20) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(um_id_str))");
            statement.execute("ALTER TABLE user_mentions ADD CONSTRAINT FK1_1__N_user_mentions FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE symbols "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE symbols ADD CONSTRAINT FK1_1__N_symbols FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");
            // Programmatically add the indices collection

            // hashtags, urls, user_mentions and symbols need to be required, so they appear in the result even if they are empty

            statement.execute("CREATE TABLE polls "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " end_datetime DATE NOT NULL, "
                    + " duration_minutes INTEGER NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE polls ADD CONSTRAINT FK1_1__N_polls FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");

            statement.execute("CREATE TABLE options "
                    + "(poll_id_str VARCHAR(20) NOT NULL, "
                    + " position INTEGER NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " PRIMARY KEY(poll_id_str, position))");
            statement.execute("ALTER TABLE options ADD CONSTRAINT FK1_1__N_options FOREIGN KEY(poll_id_str) REFERENCES polls(id_str)");

            statement.execute("CREATE TABLE media "
                    + "(id INTEGER NOT NULL, "
                    + " ID_STR VARCHAR(20) NOT NULL, "
                    + " display_url VARCHAR(1024) NOT NULL, "
                    + " expanded_url VARCHAR(1024) NOT NULL, "
                    + " media_url VARCHAR(1024) NOT NULL, "
                    + " media_url_https VARCHAR(1024) NOT NULL, "
                    + " sizes VARCHAR(20) NOT NULL, "
                    + " source_status_id INTEGER, "
                    + " source_status_id_str VARCHAR(20), "
                    + " type VARCHAR(20), "
                    + " url VARCHAR(1024) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE media ADD CONSTRAINT FK1_1__N_media FOREIGN KEY(entities_id_str) REFERENCES entities_container(id_str)");
            // Programmatically add the indices collection

            statement.execute(
                "CREATE TABLE size "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + "resize VARCHAR(10) NOT NULL, "
                    + "h INTEGER NOT NULL, "
                    + "w INTEGER NOT NULL, "
                    + " PRIMARY KEY(id_str))");

            statement.execute(
                "CREATE TABLE sizes "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + "thumb VARCHAR(20) NOT NULL, "
                    + "large VARCHAR(20) NOT NULL, "
                    + "medium VARCHAR(20) NOT NULL, "
                    + "small VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE sizes ADD CONSTRAINT FK1_1__1_thumb FOREIGN KEY(thumb) REFERENCES size(id_str)");
            statement.execute("ALTER TABLE sizes ADD CONSTRAINT FK1_1__1_large FOREIGN KEY(large) REFERENCES size(id_str)");
            statement.execute("ALTER TABLE sizes ADD CONSTRAINT FK1_1__1_medium FOREIGN KEY(medium) REFERENCES size(id_str)");
            statement.execute("ALTER TABLE sizes ADD CONSTRAINT FK1_1__1_small FOREIGN KEY(small) REFERENCES size(id_str)");

            connection.commit();
        }
    }

    @Test
    public void testParallelQueryToRoot() {

    }
}
