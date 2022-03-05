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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.xor.AssociationSetting;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.SchemaExtension;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.view.AggregateView;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class TwitterJDBCTest
{
    @Autowired
    protected AggregateManager am;

    @Autowired
    protected DataSource dataSource;

    private void addAdditionalRelationships() {
        DataModel das = am.getDataModel();

        SchemaExtension extension = new SchemaExtension()
        {
            @Override public void extend (Shape shape)
            {
                // TODO: The following are initialized attributes, i.e., they should always be included
                // user.description.urls
                // tweet.entities.symbols
                // tweet.entities.urls
                // tweet.entities.hashtags
                // tweet.entities.user_mentions

                // Add the following foreign keys
                // url 1:1 from entities_user to user_url
                // description 1:1 from entities_user to user_description
                // urls 1:N from user_url to urls
                // urls 1:N from user_description to urls
                // urls 1:N from entities_tweet to urls

                JDBCType entitiesUserType = (JDBCType) shape.getType("entities_user");
                JDBCType userUrlType = (JDBCType) shape.getType("user_url");
                JDBCType userDescType = (JDBCType) shape.getType("user_description");
                JDBCProperty userUrlPK = (JDBCProperty) ClassUtil.getDelegate(userUrlType.getProperty("ID_STR"));
                JDBCDataModel.ForeignKey fk = new JDBCDataModel.ForeignKey("FK1_1__1_url", userUrlType.getTableInfo(), entitiesUserType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty entities = new JDBCProperty("entities", userUrlPK.getColumns(), entitiesUserType, userUrlType, fk);
                entities.initMappedBy(das.getShape());
                userUrlType.addProperty(entities);

                JDBCProperty userDescPK = (JDBCProperty)ClassUtil.getDelegate(userDescType.getProperty("ID_STR"));
                fk = new JDBCDataModel.ForeignKey("FK1_1__1_description", userDescType.getTableInfo(), entitiesUserType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                entities = new JDBCProperty("entities", userDescPK.getColumns(), entitiesUserType, userDescType, fk);
                entities.initMappedBy(das.getShape());
                userDescType.addProperty(entities);

                JDBCType urlsType = (JDBCType) shape.getType("urls");
                JDBCProperty urlsPK = (JDBCProperty)ClassUtil.getDelegate(urlsType.getProperty("ID_STR"));
                // Create a synthetic foreign key between urls and user_url
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_urls", urlsType.getTableInfo(), userUrlType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty userurl = new JDBCProperty("userurl", urlsPK.getColumns(), userUrlType, urlsType, fk);
                userurl.initMappedBy(das.getShape());
                JDBCProperty position = (JDBCProperty)ClassUtil.getDelegate(urlsType.getProperty("POSITION"));
                ((JDBCProperty)ClassUtil.getDelegate(userUrlType.getProperty("urls"))).setIndexPositionProperty(position);

                // Create a synthetic foreign key between urls and user_description
                fk = new JDBCDataModel.ForeignKey("FK2_1__N_urls", urlsType.getTableInfo(), userDescType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty userdesc = new JDBCProperty("userdesc", urlsPK.getColumns(), userDescType, urlsType, fk);
                userdesc.initMappedBy(das.getShape());

                JDBCType entitiesTweetType = (JDBCType) shape.getType("entities_tweet");
                // Create a synthetic foreign key between urls and entities_tweet
                fk = new JDBCDataModel.ForeignKey("FK3_1__N_urls", urlsType.getTableInfo(), entitiesTweetType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty entitiesTweet = new JDBCProperty("entitiestweet", urlsPK.getColumns(), entitiesTweetType, urlsType, fk);
                entitiesTweet.initMappedBy(das.getShape());

                urlsType.addProperty(userurl);
                urlsType.addProperty(userdesc);
                urlsType.addProperty(entitiesTweet);

                // Add following properties
                // indices 1:N from hashtags to indices
                // indices 1:N from urls to indices
                // indices 1:N from user_mentions to indices
                // indices 1:N from symbols to indices
                // indices 1:N from media to indices

                JDBCType indicesType = (JDBCType) shape.getType("indices");
                JDBCType hashtagsType = (JDBCType) shape.getType("hashtags");
                JDBCProperty indicesPK = (JDBCProperty)ClassUtil.getDelegate(indicesType.getProperty("ID_STR"));
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_indices", indicesType.getTableInfo(), hashtagsType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty hashtags = new JDBCProperty("hashtags", indicesPK.getColumns(), hashtagsType, indicesType, fk);
                hashtags.initMappedBy(das.getShape());

                fk = new JDBCDataModel.ForeignKey("FK1_1__N_indices", indicesType.getTableInfo(), urlsType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty urls = new JDBCProperty("urls", indicesPK.getColumns(), urlsType, indicesType, fk);
                urls.initMappedBy(das.getShape());

                JDBCType mentionsType = (JDBCType) shape.getType("user_mentions");
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_indices", indicesType.getTableInfo(), mentionsType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty user_mentions = new JDBCProperty("user_mentions", indicesPK.getColumns(), mentionsType, indicesType, fk);
                user_mentions.initMappedBy(das.getShape());

                JDBCType symbolsType = (JDBCType) shape.getType("symbols");
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_indices", indicesType.getTableInfo(), symbolsType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty symbols = new JDBCProperty("symbols", indicesPK.getColumns(), symbolsType, indicesType, fk);
                symbols.initMappedBy(das.getShape());

                JDBCType mediaType = (JDBCType) shape.getType("media");
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_indices", indicesType.getTableInfo(), mediaType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                fk.makeComposition();
                JDBCProperty media = new JDBCProperty("media", indicesPK.getColumns(), mediaType, indicesType, fk);
                media.initMappedBy(das.getShape());

                indicesType.addProperty(hashtags);
                indicesType.addProperty(urls);
                indicesType.addProperty(user_mentions);
                indicesType.addProperty(symbols);
                indicesType.addProperty(media);
            }
        };

        // Rebuild the types
        das.removeShape(DataModel.DEFAULT_SHAPE);
        das.createShape(DataModel.DEFAULT_SHAPE, extension);
    }

    @BeforeEach
    public void init() throws SQLException, ClassNotFoundException, IOException
    {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {

            // Placeholder to represent an object of inverse collection relationships
            statement.execute("CREATE TABLE entities_user "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            // Programmatically add the url attribute to user_url
            // Programmatically add the description attribute to user_description

            statement.execute("CREATE TABLE user_url "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            // Programmatically add the urls collection

            statement.execute("CREATE TABLE user_description "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            // Programmatically add the urls collection

            statement.execute("CREATE TABLE user "
                    + "(id INTEGER NOT NULL, "
                    + " ID_STR VARCHAR(20) NOT NULL, "
                    + " name VARCHAR(50) NOT NULL, "
                    + " screen_name VARCHAR(15) NOT NULL, "
                    + " entities VARCHAR(20), "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE user ADD CONSTRAINT FK1_1__1_user FOREIGN KEY(entities) REFERENCES entities_user(id_str)");

            // Placeholder to represent an object of inverse collection relationships
            statement.execute("CREATE TABLE entities_tweet "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            // Programmatically add the urls collection

            statement.execute("CREATE TABLE tweet "
                    + "(created_at TIMESTAMP NOT NULL, "
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
            statement.execute("ALTER TABLE tweet ADD CONSTRAINT FK1_1__1_tweet FOREIGN KEY(entities) REFERENCES entities_tweet(id_str)");
            statement.execute("ALTER TABLE tweet ADD CONSTRAINT FK2_1__N_tweets FOREIGN KEY(user) REFERENCES user(id_str)");

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
                    + " value INTEGER, "
                    + " position INTEGER, "
                    + " PRIMARY KEY(id_str))");

            statement.execute("CREATE TABLE hashtags "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE hashtags ADD CONSTRAINT FK1_1__N_hashtags FOREIGN KEY(entities_id_str) REFERENCES entities_tweet(id_str)");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE urls "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " position INTEGER NOT NULL, "
                    + " url VARCHAR(1024) NOT NULL, "
                    + " display_url VARCHAR(1024) NOT NULL, "
                    + " expanded_url VARCHAR(1024) NOT NULL, "
                    + " PRIMARY KEY(id_str, position))");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE user_mentions "
                    + "(UM_ID_STR VARCHAR(20) NOT NULL, "
                    + " name VARCHAR(280) NOT NULL, "
                    + " id INTEGER NOT NULL, "
                    + " id_str VARCHAR(20) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(um_id_str))");
            statement.execute("ALTER TABLE user_mentions ADD CONSTRAINT FK1_1__N_user_mentions FOREIGN KEY(entities_id_str) REFERENCES entities_tweet(id_str)");
            // Programmatically add the indices collection

            statement.execute("CREATE TABLE symbols "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " text VARCHAR(280) NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE symbols ADD CONSTRAINT FK1_1__N_symbols FOREIGN KEY(entities_id_str) REFERENCES entities_tweet(id_str)");
            // Programmatically add the indices collection

            // hashtags, urls, user_mentions and symbols need to be required, so they appear in the result even if they are empty

            statement.execute("CREATE TABLE polls "
                    + "(ID_STR VARCHAR(20) NOT NULL, "
                    + " end_datetime DATE NOT NULL, "
                    + " duration_minutes INTEGER NOT NULL, "
                    + " entities_id_str VARCHAR(20) NOT NULL, "
                    + " PRIMARY KEY(id_str))");
            statement.execute("ALTER TABLE polls ADD CONSTRAINT FK1_1__N_polls FOREIGN KEY(entities_id_str) REFERENCES entities_tweet(id_str)");

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
            statement.execute("ALTER TABLE media ADD CONSTRAINT FK1_1__N_media FOREIGN KEY(entities_id_str) REFERENCES entities_tweet(id_str)");
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

            addAdditionalRelationships();
        }
    }

    @AfterEach
    public void destroy() throws SQLException, ClassNotFoundException, IOException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
            statement.executeUpdate("DROP TABLE sizes");
            statement.executeUpdate("DROP TABLE size");
            statement.executeUpdate("DROP TABLE media");
            statement.executeUpdate("DROP TABLE options");
            statement.executeUpdate("DROP TABLE polls");
            statement.executeUpdate("DROP TABLE symbols");
            statement.executeUpdate("DROP TABLE user_mentions");
            statement.executeUpdate("DROP TABLE urls");
            statement.executeUpdate("DROP TABLE hashtags");
            statement.executeUpdate("DROP TABLE indices");
            statement.executeUpdate("DROP TABLE retweettweet");
            statement.executeUpdate("DROP TABLE quotetweet");
            statement.executeUpdate("DROP TABLE tweet");
            statement.executeUpdate("DROP TABLE entities_tweet");
            statement.executeUpdate("DROP TABLE user");
            statement.executeUpdate("DROP TABLE user_description");
            statement.executeUpdate("DROP TABLE user_url");
            statement.executeUpdate("DROP TABLE entities_user");

            connection.commit();
        }

        DataModel das = am.getDataModel();
        das.removeShape("_DEFAULT_");
    }

    @Test
    public void testUrlCreate() {
        DataModel das = am.getDataModel();
        Shape shape = das.getShape();

        am.configure(null);
        JDBCSessionContext sc = ((JDBCDataStore)am.getDataStore()).getSessionContext();
        sc.beginTransaction();

        // TODO: ID should be propagated in a composition relationship
        JSONObject url = new JSONObject().put("ID_STR", "1001");
        JSONObject url1 = new JSONObject().put("ID_STR", "URL1");
        url1.put("URL", "https://t.co/LinkToTweet");
        url1.put("EXPANDED_URL", "https:\\/\\/twitter.com\\/OriginalTweeter\\/status\\/994281226797137920");
        url1.put("DISPLAY_URL", "twitter.com\\/OriginalTweeter\\/status\\/994281226797137920");

        JSONObject url2 = new JSONObject().put("ID_STR", "URL2");
        url2.put("URL", "https://t.co/T9MBCHZWcD");
        url2.put("EXPANDED_URL", "https://twitter.com/TwitterDev/status/1125490788736032770/photo/1");
        url2.put("DISPLAY_URL", "pic.twitter.com/T9MBCHZWcD");

        JSONArray urls = new JSONArray();
        urls.put(url1);
        urls.put(url2);
        url.put("urls", urls);

        // Create the urls object
        JDBCType type = (JDBCType) shape.getType("user_url");
        Settings settings = new Settings();
        settings.setEntityType(type);
        settings.init(shape);
        Object obj = am.create(url, settings);

        AggregateView view = new AggregateView();
        List<String> attributes = new ArrayList<>();
        view.setAttributeList(attributes);
        attributes.add("ID_STR");
        attributes.add("urls.ID_STR");
        attributes.add("urls.URL");
        attributes.add("urls.EXPANDED_URL");
        attributes.add("urls.DISPLAY_URL");

        settings = new Settings();
        settings.setEntityType(type);
        settings.setView(view);
        settings.init(shape);

        List<?> toList = am.query(new JSONObject().put("ID_STR", "1001"), settings);
        assert(toList.size() == 1);
        JSONObject userurl = (JSONObject)toList.get(0);
        JSONArray urlsArray = userurl.getJSONArray("urls");
        assert(urlsArray != null);
        assert(urlsArray.length() == 2);
        sc.close();
    }

    private JSONObject getTweet(boolean isQuote, int id) {
        // Create tweet object with entities populated
        JSONObject user = new JSONObject().put("ID", 1001);
        user.put("ID_STR", "1001");
        user.put("NAME", "Tweet Tester");
        user.put("SCREEN_NAME", "TweetTester");

        JSONObject tweet = new JSONObject().put("ID", id);
        tweet.put("ID_STR", (new Integer(id)).toString());
        tweet.put("CREATED_AT", new Date());
        tweet.put("TEXT", "This is a sample tweet");
        tweet.put("SOURCE", "http://www.test.com");
        tweet.put("TRUNCATED", true);
        tweet.put("USER", user);
        tweet.put("IS_QUOTE_STATUS", isQuote);
        tweet.put("REPLY_COUNT", 0);
        tweet.put("RETWEET_COUNT", 0);
        tweet.put("ENTITIES", new JSONObject());

        return tweet;
    }

    @Test
    public void testCompositionCreate() {
        DataModel das = am.getDataModel();
        Shape shape = das.getShape();

        JSONObject tweet = getTweet(false, 10001);
        JDBCType type = (JDBCType) shape.getType("tweet");
        Settings settings = new Settings();
        settings.setEntityType(type);
        settings.init(shape);
        Object obj = am.create(tweet, settings);

        assert(obj != null);
    }

    @Test
    public void testInheritanceCreate() {
        DataModel das = am.getDataModel();
        Shape shape = das.getShape();

        // Create a quotetweet object
        // 1. First create a tweet object
        // 2. Create a quote tweet referencing the tweet created in step 1

        JSONObject quotedTweet = getTweet(false, 10001);
        JSONObject quoteTweet = getTweet(true, 10002);
        quoteTweet.put("TEXT", "This is a quote tweet");
        quoteTweet.put("QUOTED_STATUS", quotedTweet);
        quoteTweet.put("QUOTED_STATUS_ID", quotedTweet.get("ID"));
        quoteTweet.put("QUOTED_STATUS_ID_STR", quotedTweet.get("ID_STR"));

        JDBCType type = (JDBCType) shape.getType("quotetweet");
        Settings settings = new Settings();
        settings.expand(new AssociationSetting("TWEET", true, false));
        settings.setEntityType(type);
        settings.init(shape);
        Object obj = am.create(quoteTweet, settings);

        // TODO: check the state graph

        assert(obj != null);
    }

    @Test
    public void testParallelQueryToRoot() {

    }
}
