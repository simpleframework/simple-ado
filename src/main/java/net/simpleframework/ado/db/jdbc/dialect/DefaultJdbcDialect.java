package net.simpleframework.ado.db.jdbc.dialect;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Properties;

import net.simpleframework.ado.EOrder;
import net.simpleframework.ado.db.common.JSqlParser;
import net.simpleframework.ado.db.common.TableColumn;
import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.FileUtils;
import net.simpleframework.common.IoUtils;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DefaultJdbcDialect extends ObjectEx implements IJdbcDialect {

	@Override
	public String toCountSQL(final String sql) {
		final StringBuilder sb = new StringBuilder();
		final String dbType = getDbType();
		if (StringUtils.hasText(dbType)) {
			isolate(new VoidIsolation() {
				@Override
				public void run() throws Exception {
					final String sql2 = JSqlParser.wrapCount(sql, dbType);
					if (sql2 != null) {
						sb.append(sql2);
					}
				}
			});
		}
		if (sb.length() == 0) {
			sb.append("select count(*) from (").append(sql).append(") t_count");
		}
		return sb.toString();
	}

	@Override
	public String toOrderBySQL(final String sql, final TableColumn... columns) {
		final StringBuilder sb = new StringBuilder();
		final String dbType = getDbType();
		if (StringUtils.hasText(dbType)) {
			isolate(new VoidIsolation() {
				@Override
				public void run() throws Exception {
					final String sql2 = JSqlParser.addOrderBy(sql, dbType, columns);
					if (sql2 != null) {
						sb.append(sql2);
					}
				}
			});
		}

		if (sb.length() == 0) {
			final StringBuilder sb2 = new StringBuilder();
			int i = 0;
			for (final TableColumn dbColumn : columns) {
				if (dbColumn.getOrder() == EOrder.normal) {
					continue;
				}
				if (i++ > 0) {
					sb2.append(",");
				}
				sb2.append(dbColumn).append(" ").append(dbColumn.getOrder());
			}
			if (sb2.length() == 0) {
				return sql;
			}
			sb.append("select * from (").append(sql).append(") t_order_by order by ").append(sb2);
		}
		return sb.toString();
	}

	@Override
	public String toConditionSQL(final String sql, final String condition) {
		final StringBuilder sb = new StringBuilder();
		final String dbType = getDbType();
		if (StringUtils.hasText(dbType)) {
			isolate(new VoidIsolation() {
				@Override
				public void run() throws Exception {
					final String sql2 = JSqlParser.addCondition(sql, dbType, condition);
					if (sql2 != null) {
						sb.append(sql2);
					}
				}
			});
		}
		if (sb.length() == 0) {
			sb.append("select * from (").append(sql).append(") t_condition where ").append(condition);
		}
		return sb.toString();
	}

	@Override
	public String toLimitSQL(final String sql, final int i, final int fetchSize) {
		return sql;
	}

	protected String getDbType() {
		return null;
	}

	@Override
	public int getResultSetType() {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public Object getResultSetValue(final ResultSet rs, final int columnIndex) throws SQLException {
		return getResultSetValue(rs, columnIndex, null);
	}

	protected Object getBlobObject(final ResultSet rs, final int columnIndex) throws SQLException {
		final Blob blob = rs.getBlob(columnIndex);
		return blob != null ? blob.getBinaryStream() : null;
	}

	protected Object getClobObject(final ResultSet rs, final int columnIndex) throws SQLException {
		final Clob clob = rs.getClob(columnIndex);
		return clob != null ? clob.getCharacterStream() : null;
	}

	@Override
	public Object getResultSetValue(final ResultSet rs, final int columnIndex,
			final Class<?> requiredType) throws SQLException {
		boolean wasNullCheck = false;
		Object obj = null;
		if (requiredType != null) {
			if (String.class.equals(requiredType)) {
				obj = rs.getString(columnIndex);
			} else if (boolean.class.equals(requiredType) || Boolean.class.equals(requiredType)) {
				obj = rs.getBoolean(columnIndex);
				wasNullCheck = true;
			} else if (byte.class.equals(requiredType) || Byte.class.equals(requiredType)) {
				obj = rs.getByte(columnIndex);
				wasNullCheck = true;
			} else if (short.class.equals(requiredType) || Short.class.equals(requiredType)) {
				obj = rs.getShort(columnIndex);
				wasNullCheck = true;
			} else if (int.class.equals(requiredType) || Integer.class.equals(requiredType)) {
				obj = rs.getInt(columnIndex);
				wasNullCheck = true;
			} else if (long.class.equals(requiredType) || Long.class.equals(requiredType)) {
				obj = rs.getLong(columnIndex);
				wasNullCheck = true;
			} else if (float.class.equals(requiredType) || Float.class.equals(requiredType)) {
				obj = rs.getFloat(columnIndex);
				wasNullCheck = true;
			} else if (double.class.equals(requiredType) || Double.class.equals(requiredType)
					|| Number.class.equals(requiredType)) {
				obj = rs.getDouble(columnIndex);
				wasNullCheck = true;
			} else if (java.sql.Date.class.equals(requiredType)) {
				obj = rs.getDate(columnIndex);
			} else if (java.sql.Time.class.equals(requiredType)) {
				obj = rs.getTime(columnIndex);
			} else if (java.sql.Timestamp.class.equals(requiredType)
					|| java.util.Date.class.equals(requiredType)) {
				obj = rs.getTimestamp(columnIndex);
			} else if (BigDecimal.class.equals(requiredType)) {
				obj = rs.getBigDecimal(columnIndex);
			} else if (byte[].class.equals(requiredType)) {
				obj = rs.getBytes(columnIndex);
			} else if (char[].class.equals(requiredType)) {
				final String str = rs.getString(columnIndex);
				if (str != null) {
					obj = str.toCharArray();
				}
			} else if (Blob.class.isAssignableFrom(requiredType)
					|| InputStream.class.isAssignableFrom(requiredType)) {
				obj = getBlobObject(rs, columnIndex);
			} else if (Clob.class.isAssignableFrom(requiredType)
					|| Reader.class.isAssignableFrom(requiredType)) {
				obj = getClobObject(rs, columnIndex);
			} else if (Properties.class.isAssignableFrom(requiredType)) {
				obj = Convert.toProperties(rs.getString(columnIndex));
			}
		}

		if (obj == null) {
			obj = rs.getObject(columnIndex);
		}

		if (obj instanceof Blob) {
			obj = ((Blob) obj).getBinaryStream();
		} else if (obj instanceof Clob) {
			obj = ((Clob) obj).getCharacterStream();
		} else if (obj instanceof java.sql.Date) {
			if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(columnIndex))) {
				obj = rs.getTimestamp(columnIndex);
			}
		}

		if (wasNullCheck && obj != null && rs.wasNull()) {
			obj = null;
		}
		return obj;
	}

	protected int getParameterType(final Class<?> paramType) {
		if (paramType == null) {
			return Types.NULL;
		}
		if (String.class.equals(paramType)) {
			return Types.VARCHAR;
		} else if (Enum.class.isAssignableFrom(paramType)) {
			// 枚举保存索引
			return Types.NUMERIC;
		} else if (boolean.class.equals(paramType) || Boolean.class.equals(paramType)) {
			// 数据库一般没有boolean类型，这里用NUMERIC替代
			return Types.NUMERIC;
		} else if (int.class.equals(paramType) || long.class.equals(paramType)
				|| double.class.equals(paramType) || float.class.equals(paramType)
				|| short.class.equals(paramType) || byte.class.equals(paramType)
				|| Number.class.isAssignableFrom(paramType)) {
			return Types.NUMERIC;
		} else if (java.util.Date.class.equals(paramType)
				|| java.util.Calendar.class.equals(paramType)) {
			return Types.TIMESTAMP;
		} else if (java.sql.Timestamp.class.equals(paramType)) {
			return Types.TIMESTAMP;
		} else if (java.sql.Date.class.equals(paramType)) {
			return Types.DATE;
		} else if (java.sql.Time.class.equals(paramType)) {
			return Types.TIME;
		} else if (byte[].class.equals(paramType) || InputStream.class.isAssignableFrom(paramType)) {
			return Types.BLOB;
		} else if (char[].class.equals(paramType) || Properties.class.equals(paramType)
				|| Reader.class.isAssignableFrom(paramType)) {
			return Types.CLOB;
		}
		return Types.NULL;
	}

	@Override
	public void setParameterValue(final PreparedStatement ps, final int paramIndex,
			final Object inValue) throws SQLException, IOException {
		final int sqlType = getParameterType(inValue == null ? null : inValue.getClass());
		boolean object = false;
		if (inValue == null) {
			ps.setNull(paramIndex, sqlType);
		} else {
			if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR) {
				ps.setString(paramIndex, inValue.toString());
			} else if (sqlType == Types.INTEGER || sqlType == Types.TINYINT
					|| sqlType == Types.SMALLINT || sqlType == Types.BIT || sqlType == Types.BIGINT
					|| sqlType == Types.FLOAT || sqlType == Types.DOUBLE || sqlType == Types.NUMERIC
					|| sqlType == Types.DECIMAL) {
				if (inValue instanceof Long) {
					ps.setLong(paramIndex, (Long) inValue);
				} else if (inValue instanceof Integer) {
					ps.setInt(paramIndex, (Integer) inValue);
				} else if (inValue instanceof Short) {
					ps.setShort(paramIndex, (Short) inValue);
				} else if (inValue instanceof Byte) {
					ps.setByte(paramIndex, (Byte) inValue);
				} else if (inValue instanceof Float) {
					ps.setFloat(paramIndex, (Float) inValue);
				} else if (inValue instanceof Double) {
					ps.setDouble(paramIndex, (Double) inValue);
				} else if (inValue instanceof BigDecimal) {
					ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
				} else {
					object = true;
				}
			} else if (sqlType == Types.BOOLEAN) {
				ps.setBoolean(paramIndex, Convert.toBool(inValue));
			} else if (sqlType == Types.DATE) {
				if (inValue instanceof java.util.Date) {
					if (inValue instanceof java.sql.Date) {
						ps.setDate(paramIndex, (java.sql.Date) inValue);
					} else {
						ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
					}
				} else if (inValue instanceof Calendar) {
					final Calendar cal = (Calendar) inValue;
					ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
				} else {
					object = true;
				}
			} else if (sqlType == Types.TIME) {
				if (inValue instanceof java.util.Date) {
					if (inValue instanceof java.sql.Time) {
						ps.setTime(paramIndex, (java.sql.Time) inValue);
					} else {
						ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
					}
				} else if (inValue instanceof Calendar) {
					final Calendar cal = (Calendar) inValue;
					ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
				} else {
					object = true;
				}
			} else if (sqlType == Types.TIMESTAMP) {
				if (inValue instanceof java.util.Date) {
					if (inValue instanceof java.sql.Timestamp) {
						ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
					} else {
						ps.setTimestamp(paramIndex,
								new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
					}
				} else if (inValue instanceof Calendar) {
					final Calendar cal = (Calendar) inValue;
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
				} else {
					object = true;
				}
			} else if (sqlType == Types.BLOB || sqlType == Types.BINARY || sqlType == Types.VARBINARY
					|| sqlType == Types.LONGVARBINARY) {
				if (inValue instanceof byte[]) {
					ps.setBytes(paramIndex, (byte[]) inValue);
				} else if (inValue instanceof InputStream) {
					if (setBinaryStreamMethod != null) {
						ClassUtils.invoke(setBinaryStreamMethod, ps, paramIndex, inValue);
					} else if (inValue instanceof ByteArrayInputStream) {
						try {
							ps.setBinaryStream(paramIndex, (InputStream) inValue,
									Convert.toInt(ClassUtils.getFieldValue("count", inValue), -1));
						} catch (final NoSuchFieldException e) {
						}
					} else if (inValue instanceof FileInputStream) {
						final FileInputStream fStream = (FileInputStream) inValue;
						ps.setBinaryStream(paramIndex, fStream, (int) fStream.getChannel().size());
					} else {
						final File file = new File(System.getProperty("java.io.tmpdir")
								+ ObjectUtils.hashStr(inValue));
						FileUtils.copyFile((InputStream) inValue, file);
						final FileInputStream fStream = new FileInputStream(file);
						ps.setBinaryStream(paramIndex, fStream, (int) fStream.getChannel().size());
					}
				} else {
					object = true;
				}
			} else if (sqlType == Types.CLOB) {
				if (inValue instanceof char[]) {
					ps.setString(paramIndex, new String((char[]) inValue));
				} else if (inValue instanceof Properties) {
					final Properties props = (Properties) inValue;
					if (props.size() > 0) {
						ps.setString(paramIndex, Convert.toString(props, null));
					} else {
						ps.setNull(paramIndex, sqlType);
					}
				} else if (inValue instanceof Reader) {
					if (setCharacterStreamMethod != null) {
						ClassUtils.invoke(setCharacterStreamMethod, ps, paramIndex, inValue);
					} else if (inValue instanceof StringReader) {
						try {
							ps.setString(paramIndex, (String) ClassUtils.getFieldValue("str", inValue));
						} catch (final NoSuchFieldException e) {
						}
					} else {
						ps.setString(paramIndex, IoUtils.getStringFromReader((Reader) inValue));
					}
				} else {
					object = true;
				}
			}
			if (object == true) {
				ps.setObject(paramIndex, inValue, sqlType);
			}
		}
	}

	private Method setBinaryStreamMethod, setCharacterStreamMethod;
	{
		try {
			// jdbc4
			setBinaryStreamMethod = PreparedStatement.class.getMethod("setBinaryStream", int.class,
					InputStream.class);
			if (Modifier.isAbstract(setBinaryStreamMethod.getModifiers())) {
				setBinaryStreamMethod = null;
			}
			setCharacterStreamMethod = PreparedStatement.class.getMethod("setCharacterStream",
					int.class, Reader.class);
			if (Modifier.isAbstract(setCharacterStreamMethod.getModifiers())) {
				setCharacterStreamMethod = null;
			}
		} catch (final NoSuchMethodException e) {
		}
	}
}
