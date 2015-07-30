package net.apnic.example.jdbcstream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class JdbcStream {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcStream(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> Stream<T> queryForStream(String sql, SqlRowMapper<T> mapper, Object... args) {
        Supplier<Spliterator<T>> supplier = () -> {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, args);
            return Spliterators.<T>spliteratorUnknownSize(new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return !rowSet.isLast();
                }

                @Override
                public T next() {
                    if (!rowSet.next()) {
                        throw new NoSuchElementException();
                    }
                    return mapper.mapRow(rowSet, rowSet.getRow());
                }
            }, Spliterator.IMMUTABLE);
        };
        return StreamSupport.stream(supplier, Spliterator.IMMUTABLE, false);
    }

    public interface SqlRowMapper<T> {
        T mapRow(SqlRowSet row, int rowNumber);
    }
}