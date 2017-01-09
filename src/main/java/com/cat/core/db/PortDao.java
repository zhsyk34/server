package com.cat.core.db;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

//TODO
public class PortDao {
	private static final String FIND_SQL = "SELECT ip, sn, port, updateTime AS happen FROM udpRecord ORDER BY id LIMIT ?, ?";
	private static final String SAVE_SQL = "INSERT INTO udpRecord(ip, sn, port, createTime, updateTime) VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE port = ?, updateTime = ?";

	public static List<UDPRecord> find(int offset, int limit) {
//		JdbcTemplate session = SqlSession.session();
//		return session.query(FIND_SQL, new Object[]{offset, limit}, (rs, rowNum) -> new UDPRecord(rs.getString("ip"), rs.getString("sn"), rs.getInt("port"), rs.getTimestamp("happen").getTime()));
		return null;
	}

	public static void save(UDPRecord record) {
		JdbcTemplate session = SqlSession.session();
		session.update(SAVE_SQL, record.getIp(), record.getSn(), record.getPort(), new Timestamp(record.getHappen()), new Timestamp(record.getHappen()), record.getIp(), record.getPort(), new Timestamp(record.getHappen()));
	}

	public static void save(List<UDPRecord> records, final int batchSize) {
		JdbcTemplate session = SqlSession.session();

		for (int i = 0; i < records.size(); i += batchSize) {
			final List<UDPRecord> list = records.subList(i, Math.min(i + batchSize, records.size()));
			session.batchUpdate(SAVE_SQL, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					UDPRecord record = list.get(i);

					int index = 1;
					ps.setString(index++, record.getIp());
					ps.setString(index++, record.getSn());
					ps.setInt(index++, record.getPort());
					ps.setTimestamp(index++, new Timestamp(record.getHappen()));
					ps.setTimestamp(index++, new Timestamp(record.getHappen()));

					ps.setString(index++, record.getIp());
					ps.setInt(index++, record.getPort());
					ps.setTimestamp(index, new Timestamp(record.getHappen()));
				}

				@Override
				public int getBatchSize() {
					return list.size();
				}
			});
		}
	}

}
