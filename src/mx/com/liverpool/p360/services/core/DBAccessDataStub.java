package mx.com.liverpool.p360.services.core;

public class DBAccessDataStub implements AutoCloseable {
	
	private final QuickJdbcConnectionManager cm = new QuickJdbcConnectionManager();
	private final RESTWorkshop rw = new RESTWorkshop();
	private final ELog log;
	
	private java.sql.Connection con = null;
	
	public DBAccessDataStub(ELog log) {
		this.log = log;
	}
	
	private void handleRefreshConnection() {
		if (con != null) {
			try {
				if (!con.isClosed() && con.isValid(1)) {
					return;
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
			closeConnection();
		}

		try {
			con = cm.openConnection(true);
		} catch (java.sql.SQLException e) {
			con = null;
			logE(e);
		}
	}

	private java.sql.Connection connection() throws java.sql.SQLException {
		handleRefreshConnection();
		if (con == null || con.isClosed()) {
			throw new java.sql.SQLException("No fue posible obtener una conexión JDBC válida");
		}
		return con;
	}

	private void closeConnection() {
		if (con == null) {
			return;
		}
		try {
			con.close();
		} catch (java.sql.SQLException e) {
			logE(e);
		} finally {
			con = null;
		}
	}
	
	public String queryDictionary(String key, String dictionary) {
		if (key == null || key.isBlank() || dictionary == null || dictionary.isBlank()) {
			return null;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb XAK1_DictionaryEntry) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"AlternativeValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"Identifier\" = ? "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, key);
			pstmnt.setNString(2, dictionary);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getString("AlternativeValue") : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public String queryVariantOrder(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		String suffix = key.replaceAll("^.+-", "");
		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"Res_Text2G_01\" "
				+ "            \"PropertyValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"Identifier\" like ? "
				+ " where aa.\"Identifier\" = 'VariantOrder' "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1";
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, "%-" + suffix);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getNString("PropertyValue") : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public java.util.Map<String, String> getDictionaryValueAlternativeValueMap(String dictionaryIdentifier) {
		java.util.Map<String, String> values = new java.util.TreeMap<>();
		if (dictionaryIdentifier == null || dictionaryIdentifier.isBlank()) {
			return values;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ " */ "
				+ "        bb.\"Identifier\" "
				+ "            \"Value\" "
				+ "       ,bb.\"AlternativeValue\" "
				+ "            \"AlternativeValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"Identifier\" is not null "
				+ " order by bb.\"Identifier\" asc"
			;
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, dictionaryIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String key = rs.getString("Value");
					if (key == null || key.isBlank()) {
						continue;
					}
					values.put( key, java.util.Objects.toString( rs.getString( "AlternativeValue"), "") );
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return values;
	}
	
	public java.util.Map<String, String> getDictionaryCharacteristicAlternativeValueMap(String dictionaryIdentifier) {
		java.util.Map<String, String> values = new java.util.TreeMap<>();
		if (dictionaryIdentifier == null || dictionaryIdentifier.isBlank()) {
			return values;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ "     index(cc XAK1_CharacteristicRevision) "
				+ " */ "
				+ "        cc.\"Identifier\" "
				+ "            \"Characteristic\" "
				+ "       ,bb.\"AlternativeValue\" "
				+ "            \"AlternativeValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cc "
				+ "    on cc.\"CharacteristicID\" = "
				+ "       bb.\"Res_Int_02\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by cc.\"Identifier\" asc"
			;
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, dictionaryIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String characteristic = rs.getString("Characteristic");
					if (characteristic == null || characteristic.isBlank()) {
						continue;
					}
					values.put( characteristic, java.util.Objects.toString( rs.getString( "AlternativeValue"), ""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return values;
	}
	
	public String getLookupValueCodeByName(String lookupIdentifier, int languageID, String name, boolean onlyActive) {

		if (lookupIdentifier == null || lookupIdentifier.isBlank() || name == null) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK2_LookupRevision) "
				+ "     index(bb XIF2_LookupValueRevision) "
				+ "     index(cc XAK1_LookupValueLang) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"Code\" "
				+ " from PIM_MAIN.\"LookupRevision\" aa "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" bb "
				+ "    on bb.\"LookupID\" = aa.\"LookupID\" "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ (onlyActive
						? "   and bb.\"IsActive\" = 1 "
						: "")
				+ " inner join PIM_MAIN.\"LookupValueLang\" cc "
				+ "    on cc.\"LookupValueRevisionID\" = bb.\"ID\" "
				+ "   and cc.\"LanguageID\" = ? "
				+ "   and cc.\"Name\" = ? "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setInt(1, languageID);
			pstmnt.setNString(2, name);
			pstmnt.setNString(3, lookupIdentifier);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getString("Code") : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public java.util.Map<String, org.json.JSONArray> getWebHierarchySynonyms(int languageID) {
		java.util.Map<String, org.json.JSONArray> synonyms = new java.util.TreeMap<>();
		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK1_StructureGroupRevision) "
				+ "     index(bb XAK1_StructureGroupLang) "
				+ "     index(cc XAK1_StructureGroupLangSynonym) "
				+ " */ "
				+ "        aa.\"Identifier\" "
				+ "            \"Identifier\" "
				+ "       ,cc.\"Name\" "
				+ "            \"Synonym\" "
				+ " from PIM_MAIN.\"StructureGroupRevision\" aa "
				+ " inner join PIM_MAIN.\"StructureGroupLang\" bb "
				+ "    on bb.\"StructureGroupRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"LanguageID\" = ? "
				+ "   and bb.\"ChannelID\" = 1 "
				+ "   and bb.\"Res_LK_Text100_01\" = 'DEFAULT' "
				+ "   and bb.\"Res_LK_Int_01\" = 0 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"StructureGroupLangSynonym\" cc "
				+ "    on cc.\"StructureGroupLangID\" = bb.\"ID\" "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"StructureID\" = 12000 "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by "
				+ "        aa.\"Identifier\" asc "
				+ "       ,cc.\"Name\" asc"
			;
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setInt(1, languageID);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getNString("Identifier");
					String synonym = rs.getNString("Synonym");
					if (identifier == null || identifier.isBlank() || synonym == null || synonym.isBlank()) {
						continue;
					}
					synonyms.computeIfAbsent( identifier, key -> new org.json.JSONArray()).put(synonym);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return synonyms;
	}
	
	public java.util.List<org.json.JSONObject> getWebHierarchyRows(int languageID) {
		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();
		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(bb XAK1_StructureGroupDetail) "
				+ "     index(cc XAK1_StructureGroupLang) "
				+ " */ "
				+ "        aa.\"ID\" "
				+ "            \"StructureGroupRevisionID\" "
				+ "       ,aa.\"StructureGroupID\" "
				+ "            \"StructureGroupID\" "
				+ "       ,aa.\"Identifier\" "
				+ "            \"Identifier\" "
				+ "       ,bb.\"ParentIdentifier\" "
				+ "            \"ParentIdentifier\" "
				+ "       ,cc.\"Name\" "
				+ "            \"Name\" "
				+ " from PIM_MAIN.\"StructureGroupRevision\" aa "
				+ " left join PIM_MAIN.\"StructureGroupDetail\" bb "
				+ "    on bb.\"StructureGroupRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"StructureGroupLang\" cc "
				+ "    on cc.\"StructureGroupRevisionID\" = aa.\"ID\" "
				+ "   and cc.\"LanguageID\" = ? "
				+ "   and cc.\"ChannelID\" = 1 "
				+ "   and cc.\"Res_LK_Text100_01\" = 'DEFAULT' "
				+ "   and cc.\"Res_LK_Int_01\" = 0 "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"StructureID\" = 12000 "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by aa.\"StructureGroupID\" asc";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setInt(1, languageID);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getNString("Identifier");

					if (identifier == null || identifier.isBlank()) {
						continue;
					}
					rows.add(
						new org.json.JSONObject()
							.put( "structureGroupRevisionID", rs.getLong("StructureGroupRevisionID"))
							.put( "structureGroupID", rs.getLong("StructureGroupID"))
							.put( "identifier", identifier)
							.put( "parentIdentifier", java.util.Objects.toString( rs.getNString("ParentIdentifier"), ""))
							.put( "name", java.util.Objects.toString( rs.getNString("Name"), "")));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	public java.util.Map<String, String> getTemplateStructureGroupAttributeValues(String template, int languageID) {
		java.util.Map<String, String> values = new java.util.TreeMap<>();
		if (template == null || template.isBlank()) {
			return values;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb dd ee cc) "
				+ "     use_nl(bb dd ee cc) "
				+ "     index(aa XAK1_StructureGroupRevision) "
				+ "     index(bb XAK1_StructureGroupAttribute) "
				+ "     index(dd XAK2_StructAttrRevision) "
				+ "     index(ee XAK1_StructureAttributeLang) "
				+ "     index(cc XAK1_SGAVal) "
				+ " */ "
				+ "        ee.\"Name\" "
				+ "            \"AttributeName\" "
				+ "       ,cc.\"Value\" "
				+ "            \"AttributeValue\" "
				+ " from PIM_MAIN.\"StructureGroupRevision\" aa "
				+ " inner join PIM_MAIN.\"StructureGroupAttribute\" bb "
				+ "    on bb.\"StructureGroupRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"StructureAttributeRevision\" dd "
				+ "    on dd.\"StructureAttributeID\" = "
				+ "       bb.\"StructureAttributeID\" "
				+ "   and dd.\"StructureID\" = aa.\"StructureID\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"StructureAttributeLang\" ee "
				+ "    on ee.\"StructureAttributeRevisionID\" = dd.\"ID\" "
				+ "   and ee.\"LanguageID\" = ? "
				+ "   and ee.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"StructureGroupAttributeVal\" cc "
				+ "    on cc.\"StructureGroupAttributeID\" = bb.\"ID\" "
				+ "   and cc.\"Identifier\" = 'DEFAULT' "
				+ "   and cc.\"LanguageID\" = ? "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"StructureID\" = 10000 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and ee.\"Name\" in ( "
				+ "       'DisplayGroupOrder', "
				+ "       'DisplayOrder', "
				+ "       'ConfigurableOrder' "
				+ "   ) "
				+ " order by ee.\"Name\" asc";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setInt(1, languageID);
			pstmnt.setInt(2, languageID);
			pstmnt.setNString(3, template);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String attributeName = rs.getString("AttributeName");
					if (attributeName == null || attributeName.isBlank()) {
						continue;
					}
					values.put(attributeName, java.util.Objects.toString(rs.getNString("AttributeValue"),""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return values;
	}

	public java.util.List<org.json.JSONObject> getStandardizationValueCharacteristicRows( String dictionaryIdentifier ) {
		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();
		if (dictionaryIdentifier == null || dictionaryIdentifier.isBlank()) {
			return rows;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb dd) "
				+ "     use_nl(bb dd) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ "     index(dd XAK1_LookupValueRevision) "
				+ " */ "
				+ "        bb.\"Res_Int_02\" "
				+ "            \"CharacteristicID\" "
				+ "       ,dd.\"Code\" "
				+ "            \"Property\" "
				+ "       ,bb.\"Res_Text2G_01\" "
				+ "            \"PropertyValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" dd "
				+ "    on dd.\"LookupValueID\" = bb.\"Res_Int_03\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"Res_Int_02\" is not null "
				+ "   and bb.\"Res_Int_03\" is not null "
				+ " order by "
				+ "        bb.\"Res_Int_02\" asc "
				+ "       ,dd.\"Code\" asc"
			;
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString( 1, dictionaryIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					rows.add( new org.json.JSONObject()
							.put( "characteristicID", rs.getInt( "CharacteristicID"))
							.put( "property", java.util.Objects.toString( rs.getString( "Property"), ""))
							.put( "propertyValue", java.util.Objects.toString( rs.getNString( "PropertyValue"), "")));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return rows;
	}
	
	public java.util.Map<Integer, String> getLookupValueCodeMap(int lookupID) {
		java.util.Map<Integer, String> values = new java.util.LinkedHashMap<>();
		String sql =
				  " select /*+ index(aa XIF2_LookupValueRevision) */ "
				+ "        aa.\"LookupValueID\" "
				+ "       ,aa.\"Code\" "
				+ " from PIM_MAIN.\"LookupValueRevision\" aa "
				+ " where aa.\"LookupID\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by aa.\"LookupValueID\" asc";
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {

			pstmnt.setInt(1, lookupID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					values.put( rs.getInt("LookupValueID"), java.util.Objects.toString( rs.getString("Code"), ""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return values;
	}
	
	public java.util.List<org.json.JSONObject> getCharacteristicMetadataRows(int languageID) {
		java.util.List<org.json.JSONObject> rows =
				new java.util.ArrayList<>();
		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(bb XAK1_CharacteristicLang) "
				+ "     index(cc XAK1_LookupRevision) "
				+ " */ "
				+ "        aa.\"CharacteristicID\" "
				+ "       ,aa.\"Identifier\" "
				+ "       ,aa.\"ParentCharacteristicID\" "
				+ "       ,bb.\"Name\" "
				+ "       ,bb.\"Description\" "
				+ "       ,aa.\"DataType\" "
				+ "       ,cc.\"Identifier\" \"LookupIdentifier\" "
				+ "       ,aa.\"IsMultiValue\" "
				+ "       ,aa.\"Purposes\" "
				+ "       ,aa.\"Order\" \"CharacteristicOrder\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " left join PIM_MAIN.\"CharacteristicLang\" bb "
				+ "    on bb.\"CharacteristicRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"LanguageID\" = ? "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupRevision\" cc "
				+ "    on cc.\"LookupID\" = aa.\"LookupID\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"DataType\" != 'NONE' "
				+ "   and aa.\"ParentCharacteristicID\" is null "
				+ " order by aa.\"Identifier\" asc";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, languageID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					Object parentCharacteristicID =
							rs.getObject("ParentCharacteristicID");

					rows.add(
						new org.json.JSONObject()
							.put( "characteristicID", rs.getInt("CharacteristicID"))
							.put( "identifier", java.util.Objects.toString( rs.getString("Identifier"), ""))
							.put( "parentCharacteristicID", parentCharacteristicID == null ? org.json.JSONObject.NULL : parentCharacteristicID)
							.put( "name", java.util.Objects.toString( rs.getNString("Name"), ""))
							.put( "description", java.util.Objects.toString( rs.getNString("Description"), ""))
							.put( "dataType", java.util.Objects.toString( rs.getString("DataType"), ""))
							.put( "lookup", java.util.Objects.toString( rs.getString("LookupIdentifier"), ""))
							.put( "isMultiValue", java.util.Objects.toString( rs.getString("IsMultiValue"), ""))
							.put( "purposesRaw", java.util.Objects.toString( rs.getString("Purposes"), ""))
							.put( "order", java.util.Objects.toString( rs.getString( "CharacteristicOrder"), "")) );
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	public String getSkuProductNo(String sku) {
		long init = System.currentTimeMillis();
		if(sku == null || sku.isEmpty())
			return null;
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				   "select /*+ leading(aa bb) use_nl(bb) */ "
				+ "   bb.\"Identifier\" "
				+ " from "
				+ "   \"ArticleDetail\" aa "
				+ " inner join "
				+ "   \"ArticleRevision\" bb "
				+ " on "
				+ "       aa.\"ArticleRevisionID\" = bb.ID "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"EntityID\" = 1100 "
				+ " where "
				+ "   aa.\"Res_Int_02\" = ?"
			)){
			pstmnt.setLong(1, Long.parseLong(sku));
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					log("From getSkuProductNo: " + rw.formatTime(System.currentTimeMillis() - init));
					return rs.getString(1);
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}catch(NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}
		log("From getSkuProductNo: " + rw.formatTime(System.currentTimeMillis() - init));
		return null;
	}
	
	public String getSkuSupplierAid(String sku) {
		long init = System.currentTimeMillis();
		if(sku == null || sku.isEmpty())
			return null;
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				"select /*+ leading(aa bb) use_nl(bb) */ "
				+ "   bb.\"Identifier\" "
				+ "from "
				+ "   \"ArticleDetail\" aa "
				+ "inner join "
				+ "   \"ArticleRevision\" bb "
				+ " on "
				+ "       aa.\"ArticleRevisionID\" = bb.ID "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"EntityID\" = 1000 "
				+ " where "
				+ "   aa.\"Res_Int_02\" = ?"
			)){
			pstmnt.setLong(1, Long.parseLong(sku));
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					log("From getSkuSupplierAid: " + rw.formatTime(System.currentTimeMillis() - init));
					return rs.getString(1);
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}catch(NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}
		log("From getSkuSupplierAid: " + rw.formatTime(System.currentTimeMillis() - init));
		return null;
	}
	
	public String[] getSkuData(String sku) {
		long init = System.currentTimeMillis();
		if(sku == null || sku.isEmpty())
			return null;
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				      " select /*+ leading(aa bb) use_nl(bb) index(aa IX_AD_TUNE_01) */ "
					+ "        bb.\"Identifier\", "
					+ "        bb.\"EntityID\" "
					+ " from "
					+ "    \"ArticleDetail\" aa "
					+ " inner join "
					+ "    \"ArticleRevision\" bb "
					+ " on "
					+ "       bb.ID = aa.\"ArticleRevisionID\" "
					+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ "   and bb.\"RevisionID\" = 1 "
					+ " where aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ "   and aa.\"Res_Int_02\" = ?"
			)){
			pstmnt.setLong(1, Long.parseLong(sku));
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					log("From getSkuSupplierAid: " + rw.formatTime(System.currentTimeMillis() - init));
					return new String[] { rs.getString(1), String.valueOf(rs.getInt(2)) } ;
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}catch(NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}
		log("From getSkuSupplierAid: " + rw.formatTime(System.currentTimeMillis() - init));
		return null;
	}
	
	private java.util.List<org.json.JSONObject> getTemplateCharacteristicMetadataRows( String template, String creationType) {
		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();
		if (template == null || template.isBlank()) {
			return rows;
		}
		if (creationType == null || creationType.isBlank()) {
			creationType = "CreateProposal";
		}
		Integer templateLVID = getLookupValueId("PPH_L4_Templates", template);
		Integer creationTypeLVID = getLookupValueId("CreationType", creationType);
		if (templateLVID == null || creationTypeLVID == null) {
			return rows;
		}
		String sql =
				  " select /*+ "
				+ "     leading(bb aa cc dd ee ff gg) "
				+ "     use_nl(aa cc dd ee ff gg) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa IX_LVREV_METADATA_EXT_01) "
				+ "     index(cc XAK1_LookupValueRevision) "
				+ "     index(dd XAK1_CharacteristicRevision) "
				+ "     index(ee XAK1_LookupValueRevision) "
				+ "     index(ff XAK1_CharacteristicLang) "
				+ "     index(gg XAK1_LookupRevision) "
				+ " */ "
				+ "        cc.\"Code\" \"StructureGroup\" "
				+ "       ,dd.\"Identifier\" \"Characteristic\" "
				+ "       ,ee.\"Code\" \"Property\" "
				+ "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
				+ "       ,ff.\"Name\" \"Name\" "
				+ "       ,ff.\"Description\" \"Description\" "
				+ "       ,dd.\"DataType\" \"DataType\" "
				+ "       ,gg.\"Identifier\" \"CharacteristicLookup\" "
				+ "       ,dd.\"IsMultiValue\" \"IsMultiValue\" "
				+ "       ,dd.\"Purposes\" \"Purposes\" "
				+ "       ,dd.\"Order\" \"CharacteristicOrder\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"Res_Int_01\" = ? "
				+ "   and aa.\"Res_Int_04\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" cc "
				+ "    on cc.\"LookupValueID\" = aa.\"Res_Int_01\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" dd "
				+ "    on dd.\"CharacteristicID\" = aa.\"Res_Int_02\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" ee "
				+ "    on ee.\"LookupValueID\" = aa.\"Res_Int_03\" "
				+ "   and ee.\"RevisionID\" = 1 "
				+ "   and ee.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"CharacteristicLang\" ff "
				+ "    on ff.\"CharacteristicRevisionID\" = dd.\"ID\" "
				+ "   and ff.\"LanguageID\" = 10 "
				+ "   and ff.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupRevision\" gg "
				+ "    on gg.\"LookupID\" = dd.\"LookupID\" "
				+ "   and gg.\"RevisionID\" = 1 "
				+ "   and gg.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where bb.\"Identifier\" = "
				+ "       'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by "
				+ "       dd.\"Identifier\" asc "
				+ "      ,ee.\"Code\" asc";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {

			pstmnt.setInt(1, templateLVID);
			pstmnt.setInt(2, creationTypeLVID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(200);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					rows.add(
						new org.json.JSONObject()
							.put( "structureGroup", java.util.Objects.toString( rs.getString("StructureGroup"), ""))
							.put( "characteristic", java.util.Objects.toString( rs.getString("Characteristic"), ""))
							.put( "property", java.util.Objects.toString( rs.getString("Property"), ""))
							.put( "propertyValue", java.util.Objects.toString( rs.getNString("PropertyValue"), ""))
							.put( "name", java.util.Objects.toString( rs.getString("Name"), ""))
							.put( "description", java.util.Objects.toString( rs.getString("Description"), ""))
							.put( "dataType", java.util.Objects.toString( rs.getString("DataType"), ""))
							.put( "lookup", java.util.Objects.toString( rs.getString( "CharacteristicLookup"), ""))
							.put( "isMultiValue", java.util.Objects.toString( rs.getString("IsMultiValue"), ""))
							.put( "purposesRaw", java.util.Objects.toString( rs.getString("Purposes"), ""))
							.put( "order", java.util.Objects.toString( rs.getString( "CharacteristicOrder"), "")));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	private java.util.Map<Integer, String> getCharacteristicPurposeCodes() {
		java.util.Map<Integer, String> purposeCodes = new java.util.LinkedHashMap<>();
		String sql =
				  " select /*+ index(aa XAK1_LookupValueRevision) */ "
				+ "        aa.\"LookupValueID\" "
				+ "       ,aa.\"Code\" "
				+ " from PIM_MAIN.\"LookupValueRevision\" aa "
				+ " where aa.\"LookupID\" = 2 "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by aa.\"LookupValueID\"";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(200);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					purposeCodes.put( rs.getInt("LookupValueID"), rs.getString("Code"));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return purposeCodes;
	}
	
	private org.json.JSONArray resolveCharacteristicPurposeCodes( String purposesRaw, java.util.Map<Integer, String> purposeCodes) {
		org.json.JSONArray purposes = new org.json.JSONArray();
		if (purposesRaw == null || purposesRaw.isBlank()) {
			return purposes;
		}
		final String entitySeparator = "[|][|]";
		final String valueSeparator = "{#}";
		for (String rawToken : purposesRaw.split(";")) {
			String token = rawToken.trim();
			if (token.isEmpty()) {
				continue;
			}
			int firstEntitySeparator = token.indexOf(entitySeparator);
			int valueSeparatorPosition = token.indexOf( valueSeparator, firstEntitySeparator   + entitySeparator.length());
			int secondEntitySeparator = token.indexOf( entitySeparator, valueSeparatorPosition + valueSeparator.length() );
			if (       firstEntitySeparator   <= 0
					|| valueSeparatorPosition <  0
					|| secondEntitySeparator  <  0) {

				log("Purpose con formato inesperado: " + token);
				continue;
			}
			try {
				int lookupID = Integer.parseInt( token.substring( 0, firstEntitySeparator));
				int lookupValueID = Integer.parseInt( token.substring( valueSeparatorPosition + valueSeparator.length(), secondEntitySeparator));
				if (lookupID != 2) {
					log( "Purpose con LookupID distinto de 2: " + token);
					continue;
				}
				String code = purposeCodes.get(lookupValueID);
				if (code == null) {
					log(
						"No se encontró Purpose Code para "
						+ "LookupValueID = "
						+ lookupValueID);

					continue;
				}
				purposes.put(code);
			} catch (NumberFormatException e) {
				log("Purpose con IDs inválidos: " + token);
			}
		}
		return purposes;
	}
	
	public java.util.Map<String, org.json.JSONObject> getTemplateCharacteristicProperties(String template) {
		return getTemplateCharacteristicProperties( template, "CreateProposal");
	}

	public java.util.Map<String, org.json.JSONObject> getTemplateCharacteristicProperties( String template, String creationType ) {
		java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = new java.util.LinkedHashMap<>();
		java.util.List<org.json.JSONObject> metadataRows = getTemplateCharacteristicMetadataRows( template, creationType );
		if (metadataRows.isEmpty()) {
			return propiedadesCaracteristicas;
		}
		java.util.Map<Integer, String> purposeCodes = getCharacteristicPurposeCodes();
		java.util.Map<String, org.json.JSONObject> metadataByCharacteristic = new java.util.LinkedHashMap<>();
		for (org.json.JSONObject row : metadataRows) {
			String characteristic = row.optString("characteristic", "");
			if (characteristic.isBlank()) {
				continue;
			}
			org.json.JSONObject properties = propiedadesCaracteristicas.computeIfAbsent( characteristic, key -> new org.json.JSONObject());
			properties.put( row.optString("property", ""), row.optString("propertyValue", "") );
			metadataByCharacteristic.putIfAbsent( characteristic, row );
		}
		for (java.util.Map.Entry<String, org.json.JSONObject> entry : metadataByCharacteristic.entrySet()) {
			String characteristic = entry.getKey();
			org.json.JSONObject row = entry.getValue();
			org.json.JSONObject properties = propiedadesCaracteristicas.get(characteristic);
			properties
				.put( "name", row.optString("name", "") )
				.put( "description", row.optString("description", "") )
				.put( "dataType", row.optString("dataType", "") )
				.put( "lookup", row.optString("lookup", "") )
				.put( "isMultiValue", row.optString("isMultiValue", "") )
				.put( "purposes", resolveCharacteristicPurposeCodes( row.optString("purposesRaw", ""), purposeCodes) )
				.put( "order", row.optString("order", "") );
		}
		return propiedadesCaracteristicas;
	}
	
	public java.util.Map<String, String> getLookupValueCodeNameMap(
			String lookupIdentifier,
			int languageID,
			boolean onlyActive) {

		java.util.Map<String, String> values =
				new java.util.TreeMap<>();

		if (lookupIdentifier == null || lookupIdentifier.isBlank()) {
			return values;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK2_LookupRevision) "
				+ "     index(bb XIF2_LookupValueRevision) "
				+ "     index(cc XAK1_LookupValueLang) "
				+ " */ "
				+ "        bb.\"Code\" "
				+ "       ,cc.\"Name\" "
				+ " from PIM_MAIN.\"LookupRevision\" aa "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" bb "
				+ "    on bb.\"LookupID\" = aa.\"LookupID\" "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ (onlyActive
						? "   and bb.\"IsActive\" = 1 "
						: "")
				+ " left join PIM_MAIN.\"LookupValueLang\" cc "
				+ "    on cc.\"LookupValueRevisionID\" = bb.\"ID\" "
				+ "   and cc.\"LanguageID\" = ? "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by bb.\"Code\" asc";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, languageID);
			pstmnt.setNString(2, lookupIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String code = rs.getString(1);

					if (code == null || code.isBlank()) {
						continue;
					}

					values.put(
							code,
							java.util.Objects.toString(
									rs.getString(2),
									""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return values;
	}
	
	public String getEanProductNo(String ean) {
		long init = System.currentTimeMillis();
		if(ean == null || ean.isEmpty())
			return null;
		handleRefreshConnection();

		String sql =
				" select /*+ leading(aa bb) use_nl(bb) index(aa XIE3_ArticleDetail) first_rows(1) */ "
			  + "    bb.\"Identifier\" "
			  + " from "
			  + "    \"ArticleDetail\" aa "
			  + " inner join "
			  + "   \"ArticleRevision\" bb "
			  + " on "
			  + "      aa.\"ArticleRevisionID\" = bb.ID "
			  + "  and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + "  and bb.\"RevisionID\" = 1 "
			  + "  and bb.\"EntityID\" = 1100 "
			  + " where "
			  + "      aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + "  and aa.\"EAN\" = ? "
			  + "  and rownum = 1";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, ean);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					log("From getEanProductNo: " + rw.formatTime(System.currentTimeMillis() - init));
					return rs.getString(1);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		log("From getEanProductNo: " + rw.formatTime(System.currentTimeMillis() - init));

		return null;
	}
	
	public String[] getEanData(String ean) {
		long init = System.currentTimeMillis();
		if(ean == null || ean.isEmpty())
			return null;
		handleRefreshConnection();

		String sql =
				" select /*+ leading(aa bb) use_nl(bb) index(aa XIE3_ArticleDetail) first_rows(1) */ "
			  + "    bb.\"Identifier\", "
			  + "    bb.\"EntityID\" "
			  + " from "
			  + "    \"ArticleDetail\" aa "
			  + " inner join "
			  + "    \"ArticleRevision\" bb "
			  + " on "
			  + "      aa.\"ArticleRevisionID\" = bb.ID "
			  + "  and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + "  and bb.\"RevisionID\" = 1 "
			  + " where "
			  + "       aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + "   and aa.\"EAN\" = ? "
			  + " order by "
			  + "    bb.\"EntityID\" asc "
			  + " fetch first 1 row only "
	    ;

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, ean);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					log("From getEanProductNo: "
							+ rw.formatTime(System.currentTimeMillis() - init));

					return new String[] { rs.getString(1) , rs.getString(2) };
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		log("From getEan: "
				+ rw.formatTime(System.currentTimeMillis() - init));

		return null;
	}
	
	private Integer getLookupValueId(String lookupIdentifier, String code) {
		String sql =
				  " select /*+ "
				+ "     leading(bb aa) "
				+ "     use_nl(aa) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa XAK2_LookupValueRevision) "
				+ " */ "
				+ "        aa.\"LookupValueID\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"Code\" = ? "
				+ " where bb.\"Identifier\" = ? "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' ";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, code);
			pstmnt.setNString(2, lookupIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	private Integer getCharacteristicId(String identifier) {
		String sql =
				  " select /*+ index(aa XAK2_CharacteristicRevision) */ "
				+ "        aa.\"CharacteristicID\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' ";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, identifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	private void fillProductLanguageData(
			org.json.JSONObject data,
			String identifier) {

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa IX_AR_TUNE_01) "
				+ "     index(bb XAK1_ArticleLang) "
				+ " */ "
				+ "        bb.\"Res_Text250_01\" \"ProductName\" "
				+ "       ,bb.\"DescriptionLong\" \"DescriptionLong\" "
				+ "       ,bb.\"Res_Text2G_01\" \"DescriptionLong2\" "
				+ " from \"ArticleRevision\" aa "
				+ " inner join \"ArticleLang\" bb "
				+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"LanguageID\" = 10 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"EntityID\" = 1100 "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, identifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					data
						.put(
							"ProductName",
							java.util.Objects.toString(
								rs.getNString("ProductName"),
								""))
						.put(
							"DescriptionLong",
							java.util.Objects.toString(
								rs.getNString("DescriptionLong"),
								""))
						.put(
							"DescriptionLong2",
							java.util.Objects.toString(
								rs.getNString("DescriptionLong2"),
								""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
	}
	
	public org.json.JSONObject getProductExtraData(String identifier, String[] characteristicIdentifiers){
		org.json.JSONObject data = 
		 new org.json.JSONObject()
			.put("product", identifier)
//			.put("supplierShopId", "")
//			.put("ProductName", "")
//			.put("BuyerRejectionMessage", "")
//			.put("SupplierRejectionMessage", "")
//			.put("SkuType", "")
//			.put("BWSCL", "")
//			.put("TImportacion", "")
//			.put("Negocio", "")
//			.put("EXTWG_S4H", "")
//			.put("MesdeEntregadeMercancIa", "")
//			.put("Temporada", "")
//			.put("BWVOR", "")
//			.put("AnoEstacion", "")
//			.put("TextoAdicional", "")
//			.put("Evento", "")
//			.put("CostobrutoSinIVA", "")
//			.put("PrecioSugeridocIVA", "")
//			.put("Descuento1", "")
//			.put("Descuento2", "")
//			.put("LABOR", "")
//			.put("NORMT", "")
//			.put("DescriptionLong", "")
//			.put("DescriptionLong2", "")
		;
		java.util.List<String> identifiers = new java.util.ArrayList<>();
		for(String characteristicIdentifier : characteristicIdentifiers) {
			if (characteristicIdentifier != null
					&& !characteristicIdentifier.isBlank()) {

				identifiers.add(characteristicIdentifier);
				data.put(characteristicIdentifier, "");
			}
		}
		String placeholders = String.join(
				",",
				java.util.Collections.nCopies(identifiers.size(), "?"));
		String sql = 
				" select /*+ "
				+ "     leading(aa bb cc dd ee) "
				+ "     use_nl(bb cc dd ee) "
				+ "     index(aa IX_AR_TUNE_01) "
				+ "     index(bb XAK1_ArticleCharactValue) "
				+ "     index(cc XAK1_CharacteristicRevision) "
				+ "     index(dd XAK1_LookupValueRevision) "
				+ "     index(ee XAK1_LookupValueLang) "
				+ " */ "
				+ "        cc.\"Identifier\" "
				+ "       ,case "
				+ "            when bb.\"Value\" is not null then bb.\"Value\" "
				+ "            else ee.\"Name\" "
				+ "        end \"CharacteristicValue\" "
				+ " from \"ArticleRevision\" aa "
				+ " inner join \"ArticleCharactValue\" bb "
				+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cc "
				+ "    on cc.\"CharacteristicID\" = bb.\"CharacteristicID\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and cc.\"Identifier\" in (" + placeholders + ") "
				+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
				+ "    on dd.\"LookupValueID\" = bb.\"LookupValueID\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" ee "
				+ "    on ee.\"LookupValueRevisionID\" = dd.\"ID\" "
				+ "   and ee.\"LanguageID\" = 10 "
				+ "   and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"EntityID\" = 1100 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'"
			;
		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			int parameterIndex = 1;

			for (String characteristicIdentifier : identifiers) {
				pstmnt.setNString(parameterIndex++, characteristicIdentifier);
			}

			pstmnt.setNString(parameterIndex, identifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String characteristicIdentifier = rs.getString(1);
					String characteristicValue = rs.getNString(2);

					data.put(
							characteristicIdentifier,
							characteristicValue == null ? "" : characteristicValue);
				}
			}
			fillProductLanguageData(data, identifier);
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return data;
	}
	
	public org.json.JSONObject getArticleExtraData(String identifier, String[] characteristicIdentifiers){
		org.json.JSONObject data = 
		 new org.json.JSONObject()
			.put("variant", identifier)
//			.put("supplierShopId", "")
//			.put("ProductName", "")
//			.put("BuyerRejectionMessage", "")
//			.put("SupplierRejectionMessage", "")
//			.put("SkuType", "")
//			.put("BWSCL", "")
//			.put("TImportacion", "")
//			.put("Negocio", "")
//			.put("EXTWG_S4H", "")
//			.put("MesdeEntregadeMercancIa", "")
//			.put("Temporada", "")
//			.put("BWVOR", "")
//			.put("AnoEstacion", "")
//			.put("TextoAdicional", "")
//			.put("Evento", "")
//			.put("CostobrutoSinIVA", "")
//			.put("PrecioSugeridocIVA", "")
//			.put("Descuento1", "")
//			.put("Descuento2", "")
//			.put("LABOR", "")
//			.put("NORMT", "")
//			.put("DescriptionLong", "")
//			.put("DescriptionLong2", "")
		;
		java.util.List<String> identifiers = new java.util.ArrayList<>();
		for(String characteristicIdentifier : characteristicIdentifiers) {
			if (characteristicIdentifier != null
					&& !characteristicIdentifier.isBlank()) {

				identifiers.add(characteristicIdentifier);
				data.put(characteristicIdentifier, "");
			}
		}
		String placeholders = String.join(
				",",
				java.util.Collections.nCopies(identifiers.size(), "?"));
		String sql = 
				" select /*+ "
				+ "     leading(aa bb cc dd ee) "
				+ "     use_nl(bb cc dd ee) "
				+ "     index(aa IX_AR_TUNE_01) "
				+ "     index(bb XAK1_ArticleCharactValue) "
				+ "     index(cc XAK1_CharacteristicRevision) "
				+ "     index(dd XAK1_LookupValueRevision) "
				+ "     index(ee XAK1_LookupValueLang) "
				+ " */ "
				+ "        cc.\"Identifier\" "
				+ "       ,case "
				+ "            when bb.\"Value\" is not null then bb.\"Value\" "
				+ "            else ee.\"Name\" "
				+ "        end \"CharacteristicValue\" "
				+ " from \"ArticleRevision\" aa "
				+ " inner join \"ArticleCharactValue\" bb "
				+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cc "
				+ "    on cc.\"CharacteristicID\" = bb.\"CharacteristicID\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and cc.\"Identifier\" in (" + placeholders + ") "
				+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
				+ "    on dd.\"LookupValueID\" = bb.\"LookupValueID\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" ee "
				+ "    on ee.\"LookupValueRevisionID\" = dd.\"ID\" "
				+ "   and ee.\"LanguageID\" = 10 "
				+ "   and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"EntityID\" = 1000 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'"
			;
		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			int parameterIndex = 1;

			for (String characteristicIdentifier : identifiers) {
				pstmnt.setNString(parameterIndex++, characteristicIdentifier);
			}

			pstmnt.setNString(parameterIndex, identifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String characteristicIdentifier = rs.getString(1);
					String characteristicValue = rs.getNString(2);

					data.put(
							characteristicIdentifier,
							characteristicValue == null ? "" : characteristicValue);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		return data;
	}

	public org.json.JSONObject getGlobalMetadata(String creationType){
		org.json.JSONObject item = new org.json.JSONObject();

		Integer euCatSystemID = getLookupValueId("ExternalSystems", "EUCat");
		if (euCatSystemID == null) {
			return item;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb ff dd gg) "
				+ "     use_nl(bb ff dd gg) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ "     index(dd XAK1_LookupValueRevision) "
				+ "     index(ff XAK1_CharacteristicRevision) "
				+ "     index(gg XAK1_LookupValueIdentifier) "
				+ " */ "
				+ "        bb.\"Identifier\" "
				+ "       ,ff.\"Identifier\" \"Characteristic\" "
				+ "       ,gg.\"Code\" \"Property\" "
				+ "       ,bb.\"Res_Text2G_01\" \"PropertyValue\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
				+ "    on dd.\"LookupValueID\" = bb.\"Res_Int_03\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueIdentifier\" gg "
				+ "    on gg.\"LookupValueRevisionID\" = dd.\"ID\" "
				+ "   and gg.\"SystemID\" = ? "
				+ "   and gg.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"CharacteristicRevision\" ff "
				+ "    on ff.\"CharacteristicID\" = bb.\"Res_Int_02\" "
				+ "   and ff.\"RevisionID\" = 1 "
				+ "   and ff.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"Res_Int_02\" is not null "
				+ "   and bb.\"Res_Int_03\" is not null "
				+ "   and bb.\"Res_Int_04\" is not null "
				+ "   and bb.\"Res_Text2G_01\" is not null "
				+ " order by bb.\"Res_Int_02\" asc ";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, euCatSystemID);
			pstmnt.setNString(2, "GlobalTemplateAttributeConfiguration");
			pstmnt.setQueryTimeout(30);

			String prevCharIdentifier = null;
			org.json.JSONObject properties = new org.json.JSONObject();

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					if (prevCharIdentifier != null
							&& !prevCharIdentifier.equals(rs.getNString(2))) {

						item.put(prevCharIdentifier, properties);
						properties = new org.json.JSONObject();
					}

					properties.put(
							rs.getString(3),
							rs.getNString(4));

					prevCharIdentifier = rs.getNString(2);
				}

				if (properties.length() > 0) {
					item.put(prevCharIdentifier, properties);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return item;
	}
	
	public org.json.JSONObject getDictionaryEntry(String diccionario, String idValor) {
		if (diccionario == null || diccionario.isBlank() || idValor == null || idValor.isBlank()) {
			return null;
		}
		Integer euCatLVID = getLookupValueId("ExternalSystems", "EUCat");
		if (euCatLVID == null) {
			return null;
		}
		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc ff ee dd gg) "
				+ "     use_nl(bb cc ff ee dd gg) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb XAK1_DictionaryEntry) "
				+ "     index(cc XAK1_LookupValueRevision) "
				+ "     index(dd XAK1_LookupValueRevision) "
				+ "     index(ee XAK1_LookupValueRevision) "
				+ "     index(ff XAK1_CharacteristicRevision) "
				+ "     index(gg XAK1_LookupValueIdentifier) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"AlternativeValue\" "
				+ "       ,cc.\"Code\" \"StructureGroup\" "
				+ "       ,ff.\"Identifier\" \"Characteristic\" "
				+ "       ,ee.\"Code\" \"CreationType\" "
				+ "       ,dd.\"Code\" \"Property\" "
				+ "       ,bb.\"Res_Text2G_01\" \"PropertyValue\" "
				+ "       ,gg.\"Code\" \"shortCode\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"Identifier\" = ? "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" cc "
				+ "    on cc.\"LookupValueID\" = bb.\"Res_Int_01\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
				+ "    on dd.\"LookupValueID\" = bb.\"Res_Int_03\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" ee "
				+ "    on ee.\"LookupValueID\" = bb.\"Res_Int_04\" "
				+ "   and ee.\"RevisionID\" = 1 "
				+ "   and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"CharacteristicRevision\" ff "
				+ "    on ff.\"CharacteristicID\" = bb.\"Res_Int_02\" "
				+ "   and ff.\"RevisionID\" = 1 "
				+ "   and ff.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueIdentifier\" gg "
				+ "    on gg.\"LookupValueRevisionID\" = dd.\"ID\" "
				+ "   and gg.\"SystemID\" = ? "
				+ "   and gg.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'"
			;

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, idValor);
			pstmnt.setInt(2, euCatLVID);
			pstmnt.setNString(3, diccionario);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}

				return new org.json.JSONObject()
						.put("diccionario", diccionario)
						.put("idValor", idValor)
						.put("alternativeValue", java.util.Objects.toString(rs.getString(1), ""))
						.put("structureGroup", java.util.Objects.toString(rs.getString(2), ""))
						.put("characteristic", java.util.Objects.toString(rs.getString(3), ""))
						.put("creationType", java.util.Objects.toString(rs.getString(4), ""))
						.put("property", java.util.Objects.toString(rs.getString(5), ""))
						.put("propertyValue", java.util.Objects.toString(rs.getNString(6), ""))
						.put("propertyShortCode", java.util.Objects.toString(rs.getString(7), ""))
					;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public org.json.JSONObject getCharacteristicData(String characteristicIdentifier) {
		if (characteristicIdentifier == null || characteristicIdentifier.isBlank()) {
			return new org.json.JSONObject()
					.put("characteristic", characteristicIdentifier)
					.put("name", "")
					.put("dataType", "")
					.put("lookup", "");
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK2_CharacteristicRevision) "
				+ "     index(bb XAK1_CharacteristicLang) "
				+ "     index(cc XAK1_LookupRevision) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"Name\" "
				+ "       ,aa.\"DataType\" "
				+ "       ,cc.\"Identifier\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " left join PIM_MAIN.\"CharacteristicLang\" bb "
				+ "    on bb.\"CharacteristicRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"LanguageID\" = 10 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupRevision\" cc "
				+ "    on cc.\"LookupID\" = aa.\"LookupID\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, characteristicIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (!rs.next()) {
					return new org.json.JSONObject()
							.put("characteristic", characteristicIdentifier)
							.put("name", "")
							.put("dataType", "")
							.put("lookup", "");
				}

				return new org.json.JSONObject()
						.put("characteristic", characteristicIdentifier)
						.put("name", java.util.Objects.toString(rs.getString(1), ""))
						.put("dataType", java.util.Objects.toString(rs.getString(2), ""))
						.put("lookup", java.util.Objects.toString(rs.getString(3), ""));
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return new org.json.JSONObject()
					.put("characteristic", characteristicIdentifier)
					.put("name", "")
					.put("dataType", "")
					.put("lookup", "");
		}
	}
	
	public String getTemplateName(String template) {
		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK1_StructureGroupRevision) "
				+ "     index(bb XAK1_StructureGroupLang) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"Name\" "
				+ " from PIM_MAIN.\"StructureGroupRevision\" aa "
				+ " inner join PIM_MAIN.\"StructureGroupLang\" bb "
				+ "    on bb.\"StructureGroupRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"LanguageID\" = 10 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1"
			;

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, template);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	private Integer getLookupValueIdByExternalCode(
			String lookupIdentifier,
			Integer systemID,
			String externalCode) {

		if (lookupIdentifier == null || lookupIdentifier.isBlank()
				|| systemID == null
				|| externalCode == null || externalCode.isBlank()) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     leading(bb aa cc) "
				+ "     use_nl(aa cc) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa XIF2_LookupValueRevision) "
				+ "     index(cc XAK1_LookupValueIdentifier) "
				+ " */ "
				+ "        aa.\"LookupValueID\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueIdentifier\" cc "
				+ "    on cc.\"LookupValueRevisionID\" = aa.\"ID\" "
				+ "   and cc.\"SystemID\" = ? "
				+ "   and cc.\"Code\" = ? "
				+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where bb.\"Identifier\" = ? "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, systemID);
			pstmnt.setNString(2, externalCode);
			pstmnt.setNString(3, lookupIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public org.json.JSONArray getTemplateCharacteristicPropertyValue(String template, String characteristic, String creationType, String property){
		org.json.JSONArray items = new org.json.JSONArray();
		if(template != null && !"".equals(template) && characteristic != null && !"".equals(characteristic) && property != null && !"".equals(property)) {
			if(creationType == null || "".equals(creationType)) {
				creationType = "CreateProposal";
			}
			handleRefreshConnection();
			Integer templateLVID = getLookupValueId("PPH_L4_Templates", template);
			Integer characteristicID = getCharacteristicId(characteristic);
			Integer creationTypeLVID = getLookupValueId("CreationType", creationType);
			Integer euCatSystemID = getLookupValueId("ExternalSystems", "EUCat");
			Integer propertyLVID =
					getLookupValueIdByExternalCode(
							"GroupCharacteristicMetadataExtensionProperty",
							euCatSystemID,
							property);
			log.log("With: " + template + ", " + characteristic + ", " + creationType + ", " + property);
			log.log("then: " + templateLVID + ", " + characteristicID + ", " + creationTypeLVID + ", " + euCatSystemID + ", " + propertyLVID);
			if(templateLVID != null && characteristicID != null && propertyLVID != null && creationTypeLVID != null) {
				String sql =
						  " select /*+ "
						+ "     leading(bb aa) "
						+ "     use_nl(aa) "
						+ "     index(bb XAK2_LookupRevision) "
						+ "     index(aa IX_LVREV_METADATA_EXT_01) "
						+ " */ "
						+ "        aa.\"Res_Text2G_01\" "
						+ " from PIM_MAIN.\"LookupRevision\" bb "
						+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
						+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
						+ "   and aa.\"Res_Int_01\" = ? "
						+ "   and aa.\"Res_Int_04\" = ? "
						+ "   and aa.\"Res_Int_02\" = ? "
						+ "   and aa.\"Res_Int_03\" = ? "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " where bb.\"Identifier\" = 'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
						+ "   and bb.\"RevisionID\" = 1 "
						+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(sql)) {

					pstmnt.setInt(1, templateLVID);
					pstmnt.setInt(2, creationTypeLVID);
					pstmnt.setInt(3, characteristicID);
					pstmnt.setInt(4, propertyLVID);
					pstmnt.setQueryTimeout(30);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						if (rs.next()) {
							items.put(
									new org.json.JSONObject()
											.put(
													characteristic,
													new org.json.JSONObject()
															.put(
																	property,
																	rs.getNString(1))));
						}
					}
				} catch (java.sql.SQLException e) {
					logE(e);
				}
			}
		}
		
		return items;
	}
	
	public org.json.JSONArray getTemplateCharacteristicPropertyValue(
			String template,
			String characteristic,
			String creationType) {

		org.json.JSONArray items = new org.json.JSONArray();

		if (template != null && !"".equals(template)
				&& characteristic != null && !"".equals(characteristic)) {

			if (creationType == null || "".equals(creationType)) {
				creationType = "CreateProposal";
			}

			handleRefreshConnection();

			Integer templateLVID =
					getLookupValueId("PPH_L4_Templates", template);

			Integer characteristicID =
					getCharacteristicId(characteristic);

			Integer creationTypeLVID =
					getLookupValueId("CreationType", creationType);

			Integer euCatSystemID =
					getLookupValueId("ExternalSystems", "EUCat");

			if (templateLVID != null
					&& characteristicID != null
					&& creationTypeLVID != null
					&& euCatSystemID != null) {

				String sql =
						  " select /*+ "
						+ "     leading(bb aa cc dd) "
						+ "     use_nl(aa cc dd) "
						+ "     index(bb XAK2_LookupRevision) "
						+ "     index(aa IX_LVREV_METADATA_EXT_01) "
						+ "     index(cc XAK1_LookupValueRevision) "
						+ "     index(dd XAK1_LookupValueIdentifier) "
						+ " */ "
						+ "        dd.\"Code\" \"Property\" "
						+ "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
						+ " from PIM_MAIN.\"LookupRevision\" bb "
						+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
						+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
						+ "   and aa.\"Res_Int_01\" = ? "
						+ "   and aa.\"Res_Int_04\" = ? "
						+ "   and aa.\"Res_Int_02\" = ? "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"LookupValueRevision\" cc "
						+ "    on cc.\"LookupValueID\" = aa.\"Res_Int_03\" "
						+ "   and cc.\"RevisionID\" = 1 "
						+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"LookupValueIdentifier\" dd "
						+ "    on dd.\"LookupValueRevisionID\" = cc.\"ID\" "
						+ "   and dd.\"SystemID\" = ? "
						+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " where bb.\"Identifier\" = 'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
						+ "   and bb.\"RevisionID\" = 1 "
						+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(sql)) {

					pstmnt.setInt(1, templateLVID);
					pstmnt.setInt(2, creationTypeLVID);
					pstmnt.setInt(3, characteristicID);
					pstmnt.setInt(4, euCatSystemID);
					pstmnt.setQueryTimeout(30);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						org.json.JSONObject properties =
								new org.json.JSONObject();

						items.put(
								new org.json.JSONObject()
										.put(characteristic, properties));

						while (rs.next()) {
							properties.put(
									rs.getString("Property"),
									rs.getNString("PropertyValue"));
						}
					}
				} catch (java.sql.SQLException e) {
					logE(e);
				}
			}
		}

		return items;
	}
	
	public org.json.JSONArray getTemplateCharacteristicPropertyValue(
			String template,
			String creationType) {

		org.json.JSONArray items = new org.json.JSONArray();

		if (template != null && !"".equals(template)) {

			if (creationType == null || "".equals(creationType)) {
				creationType = "CreateProposal";
			}

			handleRefreshConnection();

			Integer templateLVID =
					getLookupValueId("PPH_L4_Templates", template);

			Integer creationTypeLVID =
					getLookupValueId("CreationType", creationType);

			Integer euCatSystemID =
					getLookupValueId("ExternalSystems", "EUCat");

			if (templateLVID != null
					&& creationTypeLVID != null
					&& euCatSystemID != null) {

				String sql =
						  " select /*+ "
						+ "     leading(bb aa cc ee dd) "
						+ "     use_nl(aa cc ee dd) "
						+ "     index(bb XAK2_LookupRevision) "
						+ "     index(aa IX_LVREV_METADATA_EXT_01) "
						+ "     index(cc XAK1_LookupValueRevision) "
						+ "     index(ee XAK1_LookupValueIdentifier) "
						+ "     index(dd XAK1_CharacteristicRevision) "
						+ " */ "
						+ "        dd.\"Identifier\" \"Characteristic\" "
						+ "       ,ee.\"Code\" \"Property\" "
						+ "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
						+ " from PIM_MAIN.\"LookupRevision\" bb "
						+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
						+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
						+ "   and aa.\"Res_Int_01\" = ? "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"LookupValueRevision\" cc "
						+ "    on cc.\"LookupValueID\" = aa.\"Res_Int_03\" "
						+ "   and cc.\"RevisionID\" = 1 "
						+ "   and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"LookupValueIdentifier\" ee "
						+ "    on ee.\"LookupValueRevisionID\" = cc.\"ID\" "
						+ "   and ee.\"SystemID\" = ? "
						+ "   and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"CharacteristicRevision\" dd "
						+ "    on dd.\"CharacteristicID\" = aa.\"Res_Int_02\" "
						+ "   and dd.\"RevisionID\" = 1 "
						+ "   and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " where bb.\"Identifier\" = 'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
						+ "   and bb.\"RevisionID\" = 1 "
						+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
						+ " order by aa.\"Res_Int_02\"";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(sql)) {

					pstmnt.setInt(1, templateLVID);
//					pstmnt.setInt(2, creationTypeLVID);
					pstmnt.setInt(2, euCatSystemID);
					pstmnt.setQueryTimeout(30);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						String prevCharIdentifier = null;

						org.json.JSONObject properties =
								new org.json.JSONObject();

						org.json.JSONObject templateCharacteristics =
								new org.json.JSONObject();

						items.put(templateCharacteristics);

						while (rs.next()) {
							String currentCharacteristic =
									rs.getString("Characteristic");

							if (prevCharIdentifier != null
									&& !prevCharIdentifier.equals(
											currentCharacteristic)) {

								templateCharacteristics.put(
										prevCharIdentifier,
										properties);

								properties = new org.json.JSONObject();
							}

							properties.put(
									rs.getString("Property"),
									rs.getNString("PropertyValue"));

							prevCharIdentifier = currentCharacteristic;
						}

						if (prevCharIdentifier != null
								&& properties.length() > 0) {

							templateCharacteristics.put(
									prevCharIdentifier,
									properties);
						}
					}
				} catch (java.sql.SQLException e) {
					logE(e);
				}
			}
		}

		return items;
	}
	
	public String getEanSupplierAid(String ean) {
		long init = System.currentTimeMillis();
		if(ean == null || ean.isEmpty())
			return null;
		handleRefreshConnection();
		String sql =
				" select /*+ leading(aa bb) use_nl(bb) index(aa XIE3_ArticleDetail) first_rows(1) */ "
			  + "    bb.\"Identifier\" "
			  + " from "
			  + "    \"ArticleDetail\" aa "
			  + " inner join "
			  + "    \"ArticleRevision\" bb "
			  + " on "
			  + "     aa.\"ArticleRevisionID\" = bb.ID "
			  + " and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + " and bb.\"RevisionID\" = 1 "
			  + " and bb.\"EntityID\" = 1000 "
			  + " where "
			  + "     aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
			  + " and aa.\"EAN\" = ? "
			  + " and rownum = 1"
			;
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, ean);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					log("From getEanSupplierAid: "
							+ rw.formatTime(System.currentTimeMillis() - init));

					return rs.getString(1);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		log("From getEanSupplierAid: "
				+ rw.formatTime(System.currentTimeMillis() - init));

		return null;
	}
	
	public String getProductByVariant(String supplierAid) {
		long init = System.currentTimeMillis();
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				  " select /*+ leading(aa bb) use_nl(bb) */ "
				+ " bb.\"RefExtArtIdentifier\" "
				+ " from "
				+ "    \"ArticleRevision\" aa "
				+ " inner join "
				+ "    \"ArticleReference\" bb "
				+ " on "
				+ "        aa.ID = bb.\"ArticleRevisionID\" "
				+ "    and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "    and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "    and aa.\"RevisionID\" = 1 "
				+ "    and aa.\"EntityID\" = 1000 "
				+ " where "
				+ "    aa.\"Identifier\" = ?"
			)){
			pstmnt.setString(1, supplierAid);
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					log("From getProductByVariant: " + rw.formatTime(System.currentTimeMillis() - init));
					return rs.getString(1);
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}
		log("From getProductByVariant: " + rw.formatTime(System.currentTimeMillis() - init));
		return null;
	}
	
	public String[] variantBySKU(String sku) {
		long init = System.currentTimeMillis();
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement("select /*+ leading(aa bb) use_nl(bb) */ "
				+ "   bb.\"Identifier\" \"ArticleIdentifier\" "
				+ " , dd.\"Identifier\" \"ProductIdentifier\" "
				+ " , ee.\"Res_Int_02\" \"ProductSKU\" "
				+ " from "
				+ " 	\"ArticleDetail\" aa "
				+ " inner join "
				+ " 	\"ArticleRevision\" bb "
				+ "  on "
				+ " 	    bb.ID = aa.\"ArticleRevisionID\" "
				+ " 	and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and bb.\"RevisionID\" = 1 "
				+ " 	and bb.\"EntityID\" = 1000 "
				+ " inner join "
				+ " 	\"ArticleReference\" cc "
				+ " on "
				+ " 	    bb.ID = cc.\"ArticleRevisionID\" "
				+ " 	and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join "
				+ " 	\"ArticleRevision\" dd "
				+ " on "
				+ " 	    cc.\"RefIntArtID\" = dd.\"ArticleID\" "
				+ " 	and cc.\"RefExtArtIdentifier\" = dd.\"Identifier\" "
				+ " 	and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and dd.\"EntityID\" = 1100 "
				+ " 	and dd.\"RevisionID\" = 1 "
				+ " inner join "
				+ " 	\"ArticleDetail\" ee "
				+ " on "
				+ " 	    dd.ID = ee.\"ArticleRevisionID\" "
				+ " 	and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where "
				+ " 	aa.\"Res_Int_02\" = ?")){
			pstmnt.setLong(1, Long.parseLong(sku));
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					log("From variantBySKU: " + rw.formatTime(System.currentTimeMillis() - init));
					return new String[] { sku, rs.getString(1), rs.getString(2), rs.getString(3) };
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}catch(NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}
		log("From variantBySKU: " + rw.formatTime(System.currentTimeMillis() - init));
		return new String[] { sku, null, null, null };
	}
	
	public String[] variantSentToEcommBySKU(String sku) {
		long init = System.currentTimeMillis();
		handleRefreshConnection();

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				   " select /*+ leading(aa bb) use_nl(bb cc dd ee) */ "
				+ "   bb.\"Identifier\" \"ArticleIdentifier\" "
				+ " , dd.\"Identifier\" \"ParentProductIdentifier\" "
				+ " , ee.\"Res_Int_02\" \"ParentProductSKU\" "
				+ " , max(ff.\"CreationTime\") \"UltimoTiempoDeEnvioEcomm\" "
				+ " from "
				+ " 	\"ArticleDetail\" aa "
				+ " inner join "
				+ " 	\"ArticleRevision\" bb "
				+ " on "
				+ " 	    bb.ID = aa.\"ArticleRevisionID\" "
				+ " 	and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and bb.\"RevisionID\" = 1 "
				+ " 	and bb.\"EntityID\" = 1000 "
				+ " inner join "
				+ " 	\"ArticleReference\" cc "
				+ " on "
				+ " 	    bb.ID = cc.\"ArticleRevisionID\" "
				+ " 	and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join "
				+ " 	\"ArticleRevision\" dd "
				+ " on "
				+ " 	    cc.\"RefIntArtID\" = dd.\"ArticleID\" "
				+ " 	and cc.\"RefExtArtIdentifier\" = dd.\"Identifier\" "
				+ " 	and dd.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and dd.\"EntityID\" = 1100 "
				+ " 	and dd.\"RevisionID\" = 1 "
				+ " inner join "
				+ " 	\"ArticleDetail\" ee "
				+ " on "
				+ " 	    dd.ID = ee.\"ArticleRevisionID\" "
				+ " 	and ee.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " left join "
				+ "    P360_EXPLOIT.\"VW_SKU_VARIND_ENVIO_ATG\" ff "
				+ " on "
				+ "     aa.\"Res_Int_02\" = ff.SKU "
				+ " where "
				+ " 	aa.\"Res_Int_02\" = ? "
				+ " group by "
				+ "   bb.\"Identifier\" "
				+ " , dd.\"Identifier\" "
				+ " , ee.\"Res_Int_02\" "
			)) {

			pstmnt.setLong(1, Long.parseLong(sku));
//			log.log("With: " + sku );
//			log.log("then: " + sku);
			
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					java.sql.Timestamp ultimoEnvio =
							rs.getTimestamp("UltimoTiempoDeEnvioEcomm");

					log("From variantSentToEcommBySKU: "
							+ rw.formatTime(System.currentTimeMillis() - init));

					return new String[] {
						sku,
						rs.getString("ArticleIdentifier"),
						rs.getString("ParentProductIdentifier"),
						rs.getString("ParentProductSKU"),
						ultimoEnvio == null ? "" : ultimoEnvio.toString()
					};
				}else {
					log.log("No data " + sku);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		} catch (NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}

		log("From variantSentToEcommBySKU: "
				+ rw.formatTime(System.currentTimeMillis() - init));

		return new String[] { sku, null, null, null, null };
	}
	
	public java.util.Set<String> getProductVariants(String identifier){
		long init = System.currentTimeMillis();
		java.util.Set<String> variants = new java.util.TreeSet<>();
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				  " select /*+ leading(aa bb) use_nl(bb) */ "
				+ "   cc.\"Identifier\" \"ArticleIdentifier\" "
				+ " from "
				+ " 	\"ArticleRevision\" aa "
				+ " inner join "
				+ " 	\"ArticleReference\" bb "
				+ "  on "
				+ " 	    aa.\"ArticleID\" = bb.\"RefIntArtID\" "
				+ " 	and	aa.\"Identifier\" = bb.\"RefExtArtIdentifier\" "
				+ " 	and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and aa.\"RevisionID\" = 1 "
				+ " 	and aa.\"EntityID\" = 1100 "
				+ " inner join "
				+ " 	\"ArticleRevision\" cc "
				+ " on "
				+ " 		bb.\"ArticleRevisionID\" = cc.ID "
				+ " 	and cc.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " 	and cc.\"RevisionID\" = 1 "
				+ " 	and cc.\"EntityID\" = 1000 "
				+ " where "
				+ " 	aa.\"Identifier\" = ?")){
			pstmnt.setString(1, identifier);
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				while(rs.next()) {
					variants.add(rs.getString(1));
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}
		log("From getProductVariants: " + rw.formatTime(System.currentTimeMillis() - init));
		return variants;
	}
	
	public org.json.JSONObject getProductData(String identifier) {
		long init = System.currentTimeMillis();
		org.json.JSONObject productData = new org.json.JSONObject()
				.put("product", identifier)
				.put("Section", "")
				.put("ItemGroup", "")
				.put("ItemGroupS4H", "")
				.put("BrandName", "")
				.put("BRAND_ID_S4H", "")
				.put("Business", "")
				.put("SKU", "")
				.put("SupplierID", "")
				.put("Template", "")
				.put("CurrentStatus", "")
				.put("AssignTakeNoTake", "")
				.put("SAPObjectType", "")
				.put("FotoTomadaLiverpool", "")
				.put("MainBarCode", "")
				.put("MainBarCodeS4H", "")
				.put("SupplierPartNumber", "")
				;
		handleRefreshConnection();
		try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				  " SELECT /*+ leading(aa bb) use_nl(bb foto_cv assign_cv) index(aa IX_AR_TUNE_01) index(foto_cv XAK1_ArticleCharactValue) index(assign_cv XAK1_ArticleCharactValue) */\r\n"
				  + "       aa.\"Identifier\" AS \"ProductNo\",\r\n"
				  + "       bb.\"EAN\",\r\n"
				  + "       bb.\"Res_Int_02\" AS \"SKU\",\r\n"
				  + "       bb.\"CurrentStatus\",\r\n"
				  + "       bus_lvr.\"Code\" AS \"Business\",\r\n"
				  + "       sec_lvr.\"Code\" AS \"Section\",\r\n"
				  + "       ig_lvr.\"Code\" AS \"ItemGroup\",\r\n"
				  + "       igs4h_lvr.\"Code\" AS \"ItemGroupS4H\",\r\n"
				  + "       brand_lvr.\"Code\" AS \"BrandName\",\r\n"
				  + "       brand_s4h_lvr.\"Code\" AS \"BRAND_ID_S4H\",\r\n"
				  + "       sap_lvr.\"Code\" AS \"SAPObjectType\",\r\n"
				  + "       sup_lvr.\"Code\" AS \"SupplierID\",\r\n"
				  + "       foto_lvr.\"Code\" AS \"FotoTomadaLiverpool\",\r\n"
				  + "       assign_lvr.\"Code\" AS \"AssignTakeNoTake\",\r\n"
				  + "       cc.\"Res_Text250_01\" AS \"SupplierPartNumber\",\r\n"
				  + "       dd.\"StructureGroupIdentifier\"\r\n"
				  + "FROM \"ArticleRevision\" aa\r\n"
				  + "INNER JOIN \"ArticleDetail\" bb\r\n"
				  + "        ON bb.\"ArticleRevisionID\" = aa.\"ID\"\r\n"
				  + "       AND bb.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN \"ArticleDomain\" cc\r\n"
				  + "       ON cc.\"ArticleRevisionID\" = aa.\"ID\"\r\n"
				  + "      AND cc.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN \"ArticleStructureMap\" dd\r\n"
				  + "       ON dd.\"ArticleRevisionID\" = aa.\"ID\"\r\n"
				  + "      AND dd.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "      AND dd.\"StructureID\" = 10000\r\n"
				  + "LEFT JOIN \"ArticleCharactValue\" foto_cv\r\n"
				  + "       ON foto_cv.\"ArticleRevisionID\" = aa.\"ID\"\r\n"
				  + "      AND foto_cv.\"CharacteristicID\" = 4473\r\n"
				  + "      AND foto_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" foto_lvr\r\n"
				  + "       ON foto_lvr.\"LookupValueID\" = foto_cv.\"LookupValueID\"\r\n"
				  + "      AND foto_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND foto_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN \"ArticleCharactValue\" assign_cv\r\n"
				  + "       ON assign_cv.\"ArticleRevisionID\" = aa.\"ID\"\r\n"
				  + "      AND assign_cv.\"CharacteristicID\" = 3227\r\n"
				  + "      AND assign_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" assign_lvr\r\n"
				  + "       ON assign_lvr.\"LookupValueID\" = assign_cv.\"LookupValueID\"\r\n"
				  + "      AND assign_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND assign_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" bus_lvr\r\n"
				  + "       ON bus_lvr.\"LookupValueID\" = bb.\"Res_Int_01\"\r\n"
				  + "      AND bus_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND bus_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sec_lvr\r\n"
				  + "       ON sec_lvr.\"LookupValueID\" = cc.\"Res_Int_02\"\r\n"
				  + "      AND sec_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND sec_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" ig_lvr\r\n"
				  + "       ON ig_lvr.\"LookupValueID\" = cc.\"Res_Int_03\"\r\n"
				  + "      AND ig_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND ig_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" igs4h_lvr\r\n"
				  + "       ON igs4h_lvr.\"LookupValueID\" = cc.\"Res_Int_04\"\r\n"
				  + "      AND igs4h_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND igs4h_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" brand_lvr\r\n"
				  + "       ON brand_lvr.\"LookupValueID\" = cc.\"Res_Int_05\"\r\n"
				  + "      AND brand_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND brand_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" brand_s4h_lvr\r\n"
				  + "       ON brand_s4h_lvr.\"LookupValueID\" = cc.\"Res_Int_06\"\r\n"
				  + "      AND brand_s4h_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND brand_s4h_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sap_lvr\r\n"
				  + "       ON sap_lvr.\"LookupValueID\" = cc.\"Res_Int_08\"\r\n"
				  + "      AND sap_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND sap_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sup_lvr\r\n"
				  + "       ON sup_lvr.\"LookupValueID\" = cc.\"Std_Int_10\"\r\n"
				  + "      AND sup_lvr.\"RevisionID\" = 1\r\n"
				  + "      AND sup_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'\r\n"
				  + "WHERE aa.\"Identifier\" = ?\r\n"
				  + "  AND aa.\"EntityID\" = 1100\r\n"
				  + "  AND aa.\"RevisionID\" = 1\r\n"
				  + "  AND aa.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'")){
			pstmnt.setString(1, identifier);
			try(java.sql.ResultSet rs = pstmnt.executeQuery()){
				if(rs.next()) {
					Object currentStatus = rs.getObject("CurrentStatus");
					productData
						.put("product", identifier)
						.put("Section", java.util.Objects.toString(rs.getString("Section"), ""))
						.put("ItemGroup", java.util.Objects.toString(rs.getString("ItemGroup"), ""))
						.put("ItemGroupS4H", java.util.Objects.toString(rs.getString("ItemGroupS4H"), ""))
						.put("BrandName", java.util.Objects.toString(rs.getString("BrandName"), ""))
						.put("BRAND_ID_S4H", java.util.Objects.toString(rs.getString("BRAND_ID_S4H"), ""))
						.put("Business", java.util.Objects.toString(rs.getString("Business"), ""))
						.put("SKU", java.util.Objects.toString(rs.getString("SKU"), ""))
						.put("SupplierID", java.util.Objects.toString(rs.getString("SupplierID"), ""))
						.put("Template", java.util.Objects.toString(rs.getString("StructureGroupIdentifier"), ""))
						.put("CurrentStatus", currentStatus == null ? "" : String.valueOf( currentStatus ))
						.put("AssignTakeNoTake", java.util.Objects.toString(rs.getString("AssignTakeNoTake"), ""))
						.put("SAPObjectType", java.util.Objects.toString(rs.getString("SAPObjectType"), ""))
						.put("FotoTomadaLiverpool", java.util.Objects.toString(rs.getString("FotoTomadaLiverpool"), ""))
						.put("MainBarCode", java.util.Objects.toString(rs.getString("EAN"), ""))
						.put("MainBarCodeS4H", "")
						.put("SupplierPartNumber", java.util.Objects.toString(rs.getString("SupplierPartNumber"), ""))
						;
				}
			}
		}catch(java.sql.SQLException e) {
			logE(e);
		}
		log("From getProductData: " + rw.formatTime(System.currentTimeMillis() - init));
		return productData;
	}
	
	public org.json.JSONObject getArticleData(String identifier) {
		long init = System.currentTimeMillis();
	    org.json.JSONObject productData = new org.json.JSONObject()
	    		.put("variant", identifier)
	    		.put("ProductNo", "")
	    		.put("ColoursLiverpoolAtt", "")
	    		.put("TamanoUnico", "")
	    		.put("ProductImage", "")
	    		.put("AssignTakeNoTake", "")
	    		.put("SKU", "")
	    		.put("MainBarCode", "")
	    		.put("MainBarCodeS4H", "")
	    		.put("SupplierPartNumber", "");
	    handleRefreshConnection();

	    try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(
	            "SELECT /*+ leading(aa) "
	          + "use_nl(bb cc assign_cv assign_lvr sec_lvr ig_lvr ar) "
	          + "index(aa IX_AR_TUNE_01) "
	          + "index(assign_cv XAK1_ArticleCharactValue) "
	          + "index(ar XAK1_ArticleReference) */ "
	          + "       aa.\"Identifier\" AS \"VariantIdentifier\", "
	          + "       bb.\"EAN\", "
	          + "       bb.\"Res_Int_02\" AS \"SKU\", "
	          + "       bb.\"Res_Text250_02\" AS \"ProductImageURL\", "
	          + "       sec_lvr.\"Code\" AS \"TamanoUnico\", "
	          + "       ig_lvr.\"Code\" AS \"Color\", "
	          + "       assign_lvr.\"Code\" AS \"AssignTakeNoTake\", "
	          + "       cc.\"Res_Text250_01\" AS \"SupplierPartNumber\", "
	          + "       ar.\"RefExtArtIdentifier\" AS \"ProductNo\" "
	          + "FROM \"ArticleRevision\" aa "
	          + "INNER JOIN \"ArticleDetail\" bb "
	          + "        ON bb.\"ArticleRevisionID\" = aa.\"ID\" "
	          + "       AND bb.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN \"ArticleDomain\" cc "
	          + "       ON cc.\"ArticleRevisionID\" = aa.\"ID\" "
	          + "      AND cc.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN \"ArticleCharactValue\" assign_cv "
	          + "       ON assign_cv.\"ArticleRevisionID\" = aa.\"ID\" "
	          + "      AND assign_cv.\"CharacteristicID\" = 3227 "
	          + "      AND assign_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" assign_lvr "
	          + "       ON assign_lvr.\"LookupValueID\" = assign_cv.\"LookupValueID\" "
	          + "      AND assign_lvr.\"RevisionID\" = 1 "
	          + "      AND assign_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sec_lvr "
	          + "       ON sec_lvr.\"LookupValueID\" = cc.\"Res_Int_01\" "
	          + "      AND sec_lvr.\"RevisionID\" = 1 "
	          + "      AND sec_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" ig_lvr "
	          + "       ON ig_lvr.\"LookupValueID\" = cc.\"Res_Int_02\" "
	          + "      AND ig_lvr.\"RevisionID\" = 1 "
	          + "      AND ig_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "LEFT JOIN \"ArticleReference\" ar "
	          + "       ON ar.\"ArticleRevisionID\" = aa.\"ID\" "
	          + "      AND ar.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
	          + "WHERE aa.\"Identifier\" = ? "
	          + "  AND aa.\"EntityID\" = 1000 "
	          + "  AND aa.\"RevisionID\" = 1 "
	          + "  AND aa.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'")) {

	        pstmnt.setString(1, identifier);

	        try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
	            if (rs.next()) {
	            	productData
		            	.put("variant", identifier)
		            	.put("ProductNo", java.util.Objects.toString(rs.getString("ProductNo"), ""))
		            	.put("SKU", java.util.Objects.toString(rs.getString("SKU"), ""))
		            	.put("ColoursLiverpoolAtt", java.util.Objects.toString(rs.getString("Color"), ""))
		            	.put("TamanoUnico", java.util.Objects.toString(rs.getString("TamanoUnico"), ""))
		            	.put("ProductImage", java.util.Objects.toString(rs.getString("ProductImageURL"), ""))
		            	.put("MainBarCode", java.util.Objects.toString(rs.getString("EAN"), ""))
		            	.put("MainBarCodeS4H", "")
		            	.put("AssignTakeNoTake", java.util.Objects.toString(rs.getString("AssignTakeNoTake"), ""))
		            	.put("SupplierPartNumber", java.util.Objects.toString(rs.getString("SupplierPartNumber"), ""))
	            	;
	            }
	        }
	    } catch (java.sql.SQLException e) {
	        logE(e);
	    }

		log("From getArticleData: " + rw.formatTime(System.currentTimeMillis() - init));
	    return productData;
	}
	
	@Override
	public void close() {
		closeConnection();
	}

	
	private void log(String message) {
		log.log(message);
	}
	
	private void logE(Exception e) {
		log.logE(e);
	}
}