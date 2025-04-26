package fctreddit.api.server.persistence;

import java.io.File;
import java.util.List;

import fctreddit.api.data.Post;
import fctreddit.api.data.Vote;
import fctreddit.api.java.Result;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * A helper class to perform POJO (Plain Old Java Objects) persistence, using Hibernate and a backing relational database.
 */
public class Hibernate {
    private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
    private SessionFactory sessionFactory;
    private static Hibernate instance;
    private static final Object LOCK = new Object();

    private Hibernate() {
        try {
            sessionFactory = new Configuration()
                    .configure(new File(HIBERNATE_CFG_FILE))
                    .buildSessionFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the Hibernate instance, initializing if necessary.
     * Requires a configuration file (hibernate.cfg.xml)
     *
     * @return
     */
    synchronized public static Hibernate getInstance() {
        if (instance == null)
            instance = new Hibernate();
        return instance;
    }

    /**
     * Persists one or more objects to storage
     *
     * @param objects - the objects to persist
     */
    public void persist(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.persist(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Gets one object from storage
     *
     * @param identifier - the objects identifier
     * @param clazz      - the class of the object that to be returned
     */
    public <T> T get(Class<T> clazz, Object identifier) {
        Transaction tx = null;
        T element = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            element = session.get(clazz, identifier);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
        return element;
    }

    /**
     * Updates one or more objects previously persisted.
     *
     * @param objects - the objects to update
     */
    public void update(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Removes one or more objects from storage
     *
     * @param objects - the objects to remove from storage
     */
    public void delete(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void deleteAll(List<?> objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void updateAll(List<?> objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public synchronized void persistVote(Post post, Vote vote) throws InterruptedException {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
                var query = session.createQuery("SELECT v FROM Vote v WHERE v.voterId = :voterId AND v.postId = :postId", Vote.class);
                Vote possibleVote = query.setParameter("voterId", vote.getVoterId())
                        .setParameter("postId", vote.getPostId())
                        .uniqueResult();
                if (possibleVote == null)
                    session.persist(vote);
                session.merge(post);
                tx.commit();

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public synchronized void deleteVote(Post post, Vote vote) throws InterruptedException {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

                var query = session.createQuery("SELECT v FROM Vote v WHERE v.voterId = :voterId AND v.postId = :postId", Vote.class);
                Vote oldVote = query.setParameter("voterId", vote.getVoterId())
                        .setParameter("postId", vote.getPostId())
                        .uniqueResult();
                if (oldVote != null)
                    session.remove(oldVote);
                session.merge(post);
                tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }


    /**
     * Performs a jpql Hibernate query (SQL dialect)
     *
     * @param <T>           The type of objects returned by the query
     * @param jpqlStatement - the jpql query statement
     * @param clazz         - the class of the objects that will be returned
     * @return - list of objects that match the query
     */
    public <T> List<T> jpql(String jpqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpqlStatement, clazz);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Performs a (native) SQL query
     *
     * @param <T>           The type of objects returned by the query
     * @param jpqlStatement - the sql query statement
     * @param clazz         - the class of the objects that will be returned
     * @return - list of objects that match the query
     */
    public <T> List<T> sql(String jpqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createNativeQuery(jpqlStatement, clazz);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }


    public void executeSQL(String sqlStatement) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            var query = session.createNativeQuery(sqlStatement);
            query.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            throw e;

        }
    }
}