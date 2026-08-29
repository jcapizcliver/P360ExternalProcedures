package mx.com.liverpool.p360.services.core;

public class DBAccessDataStub implements AutoCloseable {
	
	private final QuickJdbcConnectionManager cm = new QuickJdbcConnectionManager();
	private final RESTWorkshop rw = new RESTWorkshop();
	private final ELog log;
	
	private java.sql.Connection con = null;
	
	public DBAccessDataStub(ELog log) {
		this.log = log;
	}
	
	public java.util.List<org.json.JSONObject> getLookupValueCodeNameExternalCodeRows(String lookupIdentifier, int languageID, String externalSystemCode, boolean onlyActive) {
		
		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();
		if (lookupIdentifier == null || lookupIdentifier.isBlank()) {
			return rows;
		}
		Integer externalSystemID = null;
		if (externalSystemCode != null
				&& !externalSystemCode.isBlank()) {
			externalSystemID = getLookupValueId("ExternalSystems", externalSystemCode);
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc dd) "
				+ "     use_nl(bb cc dd) "
				+ "     index(aa XAK2_LookupRevision) "
				+ "     index(bb XIF2_LookupValueRevision) "
				+ "     index(cc XAK1_LookupValueLang) "
				+ "     index(dd XAK1_LookupValueIdentifier) "
				+ " */ "
				+ "        bb.\"Code\" "
				+ "            \"Code\" "
				+ "       ,cc.\"Name\" "
				+ "            \"Name\" "
				+ "       ,dd.\"Code\" "
				+ "            \"ExternalCode\" "
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
				+ " left join PIM_MAIN.\"LookupValueIdentifier\" dd "
				+ "    on dd.\"LookupValueRevisionID\" = bb.\"ID\" "
				+ "   and dd.\"SystemID\" = ? "
				+ "   and dd.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by bb.\"Code\" asc";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setInt(1, languageID);
			if (externalSystemID == null) {
				pstmnt.setNull(2, java.sql.Types.NUMERIC);
			} else {
				pstmnt.setInt(2, externalSystemID);
			}
			pstmnt.setNString(3, lookupIdentifier);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String code = rs.getString("Code");
					if (code == null || code.isBlank()) {
						continue;
					}
					rows.add(
						new org.json.JSONObject()
							.put( "code", code)
							.put( "name", java.util.Objects.toString( rs.getString("Name"), "" ))
							.put( "externalCode", java.util.Objects.toString( rs.getString("ExternalCode"), "" )));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	public java.util.List<org.json.JSONObject> getArticleValidityDataByProduct(
			String productIdentifier) {

		java.util.List<org.json.JSONObject> result =
				new java.util.ArrayList<>();

		if (productIdentifier == null || productIdentifier.isBlank()) {
			return result;
		}

		handleRefreshConnection();

		String sql =
				  " select "
				+ "        article_ar.\"Identifier\" \"ArticleIdentifier\" "
				+ "       ,article_ad.\"EAN\" \"EAN\" "
				+ "       ,article_ad.\"Res_Int_02\" \"SKU\" "
				+ "       ,supplier_lvr.\"Code\" \"SupplierID\" "
				+ "       ,max(case "
				+ "            when acv.\"CharacteristicID\" = 4383 "
				+ "            then acv.\"Value\" "
				+ "        end) \"FechaInicioVigenciaCostoNeto\" "
				+ "       ,max(case "
				+ "            when acv.\"CharacteristicID\" = 4382 "
				+ "            then acv.\"Value\" "
				+ "        end) \"FechaInicioVigenciaCostoImportacion\" "
				+ "       ,max(case "
				+ "            when acv.\"CharacteristicID\" = 4384 "
				+ "            then acv.\"Value\" "
				+ "        end) \"FechaInicioVigenciaPrecioVenta\" "
				+ " from \"ArticleRevision\" product_ar "
				+ " inner join \"ArticleReference\" article_ref "
				+ "    on article_ref.\"RefIntArtID\" = product_ar.\"ArticleID\" "
				+ "   and article_ref.\"RefExtArtIdentifier\" = product_ar.\"Identifier\" "
				+ "   and article_ref.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join \"ArticleRevision\" article_ar "
				+ "    on article_ar.\"ID\" = article_ref.\"ArticleRevisionID\" "
				+ "   and article_ar.\"EntityID\" = 1000 "
				+ "   and article_ar.\"RevisionID\" = 1 "
				+ "   and article_ar.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleDetail\" article_ad "
				+ "    on article_ad.\"ArticleRevisionID\" = article_ar.\"ID\" "
				+ "   and article_ad.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleDomain\" product_dom "
				+ "    on product_dom.\"ArticleRevisionID\" = product_ar.\"ID\" "
				+ "   and product_dom.\"EntityID\" = 21006 "
				+ "   and product_dom.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" supplier_lvr "
				+ "    on supplier_lvr.\"LookupValueID\" = product_dom.\"Std_Int_10\" "
				+ "   and supplier_lvr.\"RevisionID\" = 1 "
				+ "   and supplier_lvr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleCharactValue\" acv "
				+ "    on acv.\"ArticleRevisionID\" = article_ar.\"ID\" "
				+ "   and acv.\"CharacteristicID\" in (4382, 4383, 4384) "
				+ "   and acv.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where product_ar.\"Identifier\" = ? "
				+ "   and product_ar.\"EntityID\" = 1100 "
				+ "   and product_ar.\"RevisionID\" = 1 "
				+ "   and product_ar.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " group by "
				+ "        article_ar.\"Identifier\" "
				+ "       ,article_ad.\"EAN\" "
				+ "       ,article_ad.\"Res_Int_02\" "
				+ "       ,supplier_lvr.\"Code\" "
				+ " order by article_ar.\"Identifier\"";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, productIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {

				while (rs.next()) {

					result.add(
						new org.json.JSONObject()
							.put(
								"ArticleIdentifier",
								java.util.Objects.toString(
									rs.getString("ArticleIdentifier"), ""))
							.put(
								"EAN",
								java.util.Objects.toString(
									rs.getString("EAN"), ""))
							.put(
								"SKU",
								java.util.Objects.toString(
									rs.getString("SKU"), ""))
							.put(
								"SupplierID",
								java.util.Objects.toString(
									rs.getString("SupplierID"), ""))
							.put(
								"FechaInicioVigenciaCostoNeto",
								java.util.Objects.toString(
									rs.getNString("FechaInicioVigenciaCostoNeto"), ""))
							.put(
								"FechaInicioVigenciaCostoImportacion",
								java.util.Objects.toString(
									rs.getNString("FechaInicioVigenciaCostoImportacion"), ""))
							.put(
								"FechaInicioVigenciaPrecioVenta",
								java.util.Objects.toString(
									rs.getNString("FechaInicioVigenciaPrecioVenta"), "")));
				}
			}

		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return result;
	}
	
	public java.util.List<String> getArticleObjectIdsByProduct(String productIdentifier) {

		java.util.List<String> items = new java.util.ArrayList<>();

		if (productIdentifier == null || productIdentifier.isBlank()) {
			return items;
		}

		handleRefreshConnection();

		String sql =
				  " select /*+ "
				+ "     leading(product_ar article_ref article_ar) "
				+ "     use_nl(article_ref article_ar) "
				+ "     index(product_ar IX_AR_TUNE_01) "
				+ " */ "
				+ "        article_ar.\"ArticleID\" "
				+ "       ,article_ar.\"CatalogID\" "
				+ " from \"ArticleRevision\" product_ar "
				+ " inner join \"ArticleReference\" article_ref "
				+ "    on article_ref.\"RefIntArtID\" = product_ar.\"ArticleID\" "
				+ "   and article_ref.\"RefIntCatID\" = product_ar.\"CatalogID\" "
				+ "   and article_ref.\"RefEntityID\" = 1100 "
				+ "   and article_ref.\"TypeID\" = 100 "
				+ "   and article_ref.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " inner join \"ArticleRevision\" article_ar "
				+ "    on article_ar.\"ID\" = article_ref.\"ArticleRevisionID\" "
				+ "   and article_ar.\"EntityID\" = 1000 "
				+ "   and article_ar.\"RevisionID\" = 1 "
				+ "   and article_ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where product_ar.\"Identifier\" = ? "
				+ "   and product_ar.\"EntityID\" = 1100 "
				+ "   and product_ar.\"CatalogID\" = 1 "
				+ "   and product_ar.\"RevisionID\" = 1 "
				+ "   and product_ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " order by article_ar.\"ArticleID\"";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {

			pstmnt.setNString(1, productIdentifier);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					items.add(
							rs.getLong(1)
							+ "@"
							+ rs.getLong(2));
				}
			}

		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return items;
	}

	/**
	 * Misma semántica que getLookupValueCodeNameExternalCodeRows(...) pero
	 * limitando LookupValue.Code en Oracle para no bajar el lookup completo.
	 *
	 * IMPORTANTE: conserva los mismos joins, hints y ExternalSystem que el
	 * método original que ya estaba funcionando.
	 */
	public java.util.List<org.json.JSONObject> getLookupValueCodeNameExternalCodeRows(
			String lookupIdentifier,
			java.util.Collection<String> allowedCodes,
			int languageID,
			String externalSystemCode,
			boolean onlyActive) {

		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();

		if (lookupIdentifier == null || lookupIdentifier.isBlank()
				|| allowedCodes == null || allowedCodes.isEmpty()) {
			return rows;
		}

		java.util.LinkedHashSet<String> normalizedCodes =
				new java.util.LinkedHashSet<>();

		for (String code : allowedCodes) {
			if (code != null && !code.isBlank()) {
				normalizedCodes.add(code.trim());
			}
		}

		if (normalizedCodes.isEmpty()) {
			return rows;
		}

		Integer externalSystemID = null;

		if (externalSystemCode != null
				&& !externalSystemCode.isBlank()) {
			externalSystemID =
					getLookupValueId(
							"ExternalSystems",
							externalSystemCode);
		}

		java.util.List<String> codes =
				new java.util.ArrayList<>(normalizedCodes);

		final int chunkSize = 900;

		for (int from = 0; from < codes.size(); from += chunkSize) {

			int to = Math.min(from + chunkSize, codes.size());

			java.util.List<String> chunk =
					codes.subList(from, to);

			String placeholders =
					String.join(
							",",
							java.util.Collections.nCopies(
									chunk.size(),
									"?"));

			String sql =
					  " select /*+ "
					+ "     leading(aa bb cc dd) "
					+ "     use_nl(bb cc dd) "
					+ "     index(aa XAK2_LookupRevision) "
					+ "     index(bb XIF2_LookupValueRevision) "
					+ "     index(cc XAK1_LookupValueLang) "
					+ "     index(dd XAK1_LookupValueIdentifier) "
					+ " */ "
					+ "        bb.\"Code\" "
					+ "            \"Code\" "
					+ "       ,cc.\"Name\" "
					+ "            \"Name\" "
					+ "       ,dd.\"Code\" "
					+ "            \"ExternalCode\" "
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
					+ " left join PIM_MAIN.\"LookupValueIdentifier\" dd "
					+ "    on dd.\"LookupValueRevisionID\" = bb.\"ID\" "
					+ "   and dd.\"SystemID\" = ? "
					+ "   and dd.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Identifier\" = ? "
					+ "   and aa.\"RevisionID\" = 1 "
					+ "   and aa.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ "   and bb.\"Code\" in (" + placeholders + ") "
					+ " order by bb.\"Code\" asc";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				int parameterIndex = 1;

				pstmnt.setInt(
						parameterIndex++,
						languageID);

				if (externalSystemID == null) {
					pstmnt.setNull(
							parameterIndex++,
							java.sql.Types.NUMERIC);
				} else {
					pstmnt.setInt(
							parameterIndex++,
							externalSystemID);
				}

				pstmnt.setNString(
						parameterIndex++,
						lookupIdentifier);

				for (String code : chunk) {
					pstmnt.setNString(
							parameterIndex++,
							code);
				}

				pstmnt.setQueryTimeout(30);

				try (java.sql.ResultSet rs =
						pstmnt.executeQuery()) {

					while (rs.next()) {

						String code =
								rs.getString("Code");

						if (code == null || code.isBlank()) {
							continue;
						}

						rows.add(
								new org.json.JSONObject()
										.put(
												"code",
												code)
										.put(
												"name",
												java.util.Objects.toString(
														rs.getString("Name"),
														""))
										.put(
												"externalCode",
												java.util.Objects.toString(
														rs.getString("ExternalCode"),
														"")));
					}
				}

			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return rows;
	}
	
	public java.util.List<org.json.JSONObject> getCharacteristicIntegrationMetadataRows() {

		java.util.List<org.json.JSONObject> rows =
				new java.util.ArrayList<>();

		Integer eccSystemID =
				getLookupValueId("ExternalSystems", "ECC");

		Integer s4hSystemID =
				getLookupValueId("ExternalSystems", "S4HANA");

		Integer atgSystemID =
				getLookupValueId("ExternalSystems", "ATG");

		if(eccSystemID == null
				|| s4hSystemID == null
				|| atgSystemID == null) {
			return rows;
		}

		handleRefreshConnection();

		String sql =
				  " select /*+ "
				+ "     leading(aa bb cc) "
				+ "     use_nl(bb cc) "
				+ "     index(aa XAK2_CharacteristicRevision) "
				+ " */ "
				+ "        aa.\"Identifier\" "
				+ "            \"Identifier\" "
				+ "       ,aa.\"DataType\" "
				+ "            \"DataType\" "
				+ "       ,max(case "
				+ "            when bb.\"SystemID\" = ? "
				+ "            then bb.\"AlternativeIdentifier\" "
				+ "        end) \"ECC\" "
				+ "       ,max(case "
				+ "            when bb.\"SystemID\" = ? "
				+ "            then bb.\"AlternativeIdentifier\" "
				+ "        end) \"S4HANA\" "
				+ "       ,max(case "
				+ "            when bb.\"SystemID\" = ? "
				+ "            then bb.\"AlternativeIdentifier\" "
				+ "        end) \"ATG\" "
				+ "       ,cc.\"Identifier\" "
				+ "            \"LookupIdentifier\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " left join PIM_MAIN.\"CharacteristicIdentifier\" bb "
				+ "    on bb.\"CharacteristicRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"SystemID\" in (?, ?, ?) "
				+ " left join PIM_MAIN.\"LookupRevision\" cc "
				+ "    on cc.\"LookupID\" = aa.\"LookupID\" "
				+ "   and cc.\"RevisionID\" = 1 "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"IsActive\" = 1 "
				+ "   and aa.\"ParentCharacteristicID\" is null "
				+ "   and aa.\"DataType\" <> 'NONE' "
				+ "   and aa.\"Entities\" is not null "
				+ " group by "
				+ "        aa.\"Identifier\" "
				+ "       ,aa.\"DataType\" "
				+ "       ,cc.\"Identifier\" "
				+ " order by aa.\"Identifier\"";

		try(java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, eccSystemID);
			pstmnt.setInt(2, s4hSystemID);
			pstmnt.setInt(3, atgSystemID);

			pstmnt.setInt(4, eccSystemID);
			pstmnt.setInt(5, s4hSystemID);
			pstmnt.setInt(6, atgSystemID);

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try(java.sql.ResultSet rs = pstmnt.executeQuery()) {

				while(rs.next()) {

					rows.add(
						new org.json.JSONObject()
							.put(
								"identifier",
								java.util.Objects.toString(
									rs.getString("Identifier"),
									""))
							.put(
								"dataType",
								java.util.Objects.toString(
									rs.getString("DataType"),
									""))
							.put(
								"ecc",
								java.util.Objects.toString(
									rs.getString("ECC"),
									""))
							.put(
								"s4hana",
								java.util.Objects.toString(
									rs.getString("S4HANA"),
									""))
							.put(
								"atg",
								java.util.Objects.toString(
									rs.getString("ATG"),
									""))
							.put(
								"lookup",
								java.util.Objects.toString(
									rs.getString("LookupIdentifier"),
									""))
					);
				}
			}

		}catch(java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	public java.util.List<org.json.JSONObject> getActiveCharacteristicsByExternalSystem(String externalSystemCode) {
	java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();
	if(externalSystemCode == null || externalSystemCode.isBlank()) {
		return rows;
	}
	Integer systemID = getLookupValueId("ExternalSystems",externalSystemCode);
	if(systemID == null) {
		return rows;
	}
	handleRefreshConnection();
	String sql =
			  " select /*+ "
		+ "     leading(bb aa) "
		+ "     use_nl(aa) "
		+ " */ "
		+ " distinct "
		+ "        aa.\"Identifier\" "
		+ "            \"Identifier\" "
		+ "       ,aa.\"Entities\" "
		+ "            \"Entities\" "
		+ " from PIM_MAIN.\"CharacteristicIdentifier\" bb "
		+ " inner join PIM_MAIN.\"CharacteristicRevision\" aa "
		+ "    on aa.\"ID\" = bb.\"CharacteristicRevisionID\" "
		+ "   and aa.\"RevisionID\" = 1 "
		+ "   and aa.\"DeletionTimestamp\" = "
		+ "       timestamp '9999-12-31 00:00:00.0' "
		+ "   and aa.\"IsActive\" = 1 "
		+ " where bb.\"SystemID\" = ? "
		+ "   and bb.\"AlternativeIdentifier\" is not null "
		+ "   and bb.\"DeletionTimestamp\" = "
		+ "       timestamp '9999-12-31 00:00:00.0' "
		+ " order by aa.\"Identifier\"";
	try(java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
	
		pstmnt.setInt(1, systemID);
		pstmnt.setQueryTimeout(30);
		pstmnt.setFetchSize(2000);
	
		try(java.sql.ResultSet rs = pstmnt.executeQuery()) {
	
			while(rs.next()) {
				rows.add(
					new org.json.JSONObject()
						.put(
							"identifier",
						java.util.Objects.toString(
							rs.getString("Identifier"),
							""))
					.put(
						"entities",
						java.util.Objects.toString(
							rs.getString("Entities"),
							""))
				);
			}
		}
	
	}catch(java.sql.SQLException e) {
		logE(e);
	}
	
	return rows;
	}
	
	public String[] getProductInfoBySKU(String sku) {

		if(sku == null || sku.isBlank()) {
			return null;
		}

		handleRefreshConnection();

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb cc bus_lvr sap_lvr) "
				+ "     index(aa IX_AD_TUNE_01) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"Identifier\" \"ProductNo\" "
				+ "       ,sap_lvr.\"Code\" \"SAPObjectType\" "
				+ "       ,bus_lvr.\"Code\" \"Business\" "
				+ " from \"ArticleDetail\" aa "
				+ " inner join \"ArticleRevision\" bb "
				+ "    on bb.\"ID\" = aa.\"ArticleRevisionID\" "
				+ "   and bb.\"EntityID\" = 1100 "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleDomain\" cc "
				+ "    on cc.\"ArticleRevisionID\" = bb.\"ID\" "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" bus_lvr "
				+ "    on bus_lvr.\"LookupValueID\" = aa.\"Res_Int_01\" "
				+ "   and bus_lvr.\"RevisionID\" = 1 "
				+ "   and bus_lvr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" sap_lvr "
				+ "    on sap_lvr.\"LookupValueID\" = cc.\"Res_Int_08\" "
				+ "   and sap_lvr.\"RevisionID\" = 1 "
				+ "   and sap_lvr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Res_Int_02\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " fetch first 1 row only";

		try(java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setLong(1, Long.parseLong(sku));
			pstmnt.setQueryTimeout(30);

			try(java.sql.ResultSet rs = pstmnt.executeQuery()) {

				if(rs.next()) {
					return new String[] {
							java.util.Objects.toString(
									rs.getString("ProductNo"),
									""),
							java.util.Objects.toString(
									rs.getString("SAPObjectType"),
									""),
							java.util.Objects.toString(
									rs.getString("Business"),
									"")
					};
				}
			}

		}catch(java.sql.SQLException e) {
			logE(e);
		}catch(NumberFormatException e) {
			log("Invalid SKU: " + sku);
		}

		return null;
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
	
	
	public java.util.Map<String, org.json.JSONObject> getProductCharacteristicValues(
			java.util.Collection<String> identifiers,
			java.util.Collection<String> characteristicIdentifiers)
			throws java.sql.SQLException {

		java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();

		java.util.List<String> characteristics = normalizeIdentifiers(characteristicIdentifiers);

		java.util.List<java.util.List<String>> idChunks = identifierChunks(identifiers);

		for (java.util.List<String> idChunk : idChunks) {
			for (String identifier : idChunk) {
				org.json.JSONObject row = new org.json.JSONObject().put("product", identifier);
				for (String characteristic : characteristics) {
					row.put(characteristic, "");
				}
				result.put(identifier, row);
			}
		}
		if (idChunks.isEmpty() || characteristics.isEmpty()) {
			return result;
		}
		handleRefreshConnection();
		for (java.util.List<String> idChunk : idChunks) {
			for (java.util.List<String> characteristicChunk : identifierChunks(characteristics)) {

				String sql =
						  " select /*+ leading(aa bb cc dd ee) "
						+ "use_nl(bb cc dd ee) "
						+ "index(aa IX_AR_TUNE_01) "
						+ "index(bb XAK1_ArticleCharactValue) "
						+ "index(cc XAK1_CharacteristicRevision) "
						+ "index(dd XAK1_LookupValueRevision) "
						+ "index(ee XAK1_LookupValueLang) */ "
						+ "        aa.\"Identifier\" \"ItemIdentifier\" "
						+ "       ,cc.\"Identifier\" \"CharacteristicIdentifier\" "
						+ "       ,case "
						+ "            when bb.\"Value\" is not null then bb.\"Value\" "
						+ "            else ee.\"Name\" "
						+ "        end \"CharacteristicValue\" "
						+ " from \"ArticleRevision\" aa "
						+ " inner join \"ArticleCharactValue\" bb "
						+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
						+ "   and bb.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"CharacteristicRevision\" cc "
						+ "    on cc.\"CharacteristicID\" = bb.\"CharacteristicID\" "
						+ "   and cc.\"RevisionID\" = 1 "
						+ "   and cc.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
						+ "    on dd.\"LookupValueID\" = bb.\"LookupValueID\" "
						+ "   and dd.\"RevisionID\" = 1 "
						+ "   and dd.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " left join PIM_MAIN.\"LookupValueLang\" ee "
						+ "    on ee.\"LookupValueRevisionID\" = dd.\"ID\" "
						+ "   and ee.\"LanguageID\" = 10 "
						+ "   and ee.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " where aa.\"Identifier\" in ("
						+ placeholders(idChunk.size())
						+ ") "
						+ "   and cc.\"Identifier\" in ("
						+ placeholders(characteristicChunk.size())
						+ ") "
						+ "   and aa.\"EntityID\" = 1100 "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0'";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(sql)) {

					bindNStrings(pstmnt, 1, idChunk);
					bindNStrings(
							pstmnt,
							idChunk.size() + 1,
							characteristicChunk);

					pstmnt.setQueryTimeout(120);
					pstmnt.setFetchSize(1000);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						while (rs.next()) {
							org.json.JSONObject row =
									result.get(rs.getString("ItemIdentifier"));
							if (row != null) {
								row.put(
									rs.getString("CharacteristicIdentifier"),
									java.util.Objects.toString(
										rs.getNString("CharacteristicValue"),
										""));
							}
						}
					}
				}
			}
		}

		return result;
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
	
	public String getProductPrimaryTemplate(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa IX_AR_TUNE_01) "
				+ "     index(bb IX_ASM_TUNE_01) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        bb.\"StructureGroupIdentifier\" "
				+ " from PIM_MASTER.\"ArticleRevision\" aa "
				+ " inner join PIM_MASTER.\"ArticleStructureMap\" bb "
				+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"StructureID\" = 10000 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"EntityID\" = 1100 "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and length(trim(bb.\"StructureGroupIdentifier\")) > 0 "
				+ "   and rownum = 1";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, identifier);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	public org.json.JSONObject getProductStatusData(String identifier) {
	    org.json.JSONObject result = new org.json.JSONObject()
	            .put("CurrentStatus", "")
	            .put("PreviousStatus", "")
	            .put("ExternalStatus", "");

	    if (identifier == null || identifier.trim().isEmpty()) {
	        return result;
	    }

	    handleRefreshConnection();

	    String sql =
	              "select /*+ leading(ar ad) "
	            + "           use_nl(ad ext_lvr) "
	            + "           index(ar IX_AR_TUNE_01) */ "
	            + "       ad.\"CurrentStatus\" as \"CurrentStatus\", "
	            + "       ad.\"Res_Int_03\" as \"PreviousStatus\", "
	            + "       ext_lvr.\"Code\" as \"ExternalStatus\" "
	            + "from \"ArticleRevision\" ar "
	            + "inner join \"ArticleDetail\" ad "
	            + "        on ad.\"ArticleRevisionID\" = ar.\"ID\" "
	            + "       and ad.\"DeletionTimestamp\" = "
	            + "           timestamp '9999-12-31 00:00:00.0' "
	            + "left join PIM_MAIN.\"LookupValueRevision\" ext_lvr "
	            + "       on ext_lvr.\"LookupValueID\" = ad.\"Res_Int_04\" "
	            + "      and ext_lvr.\"RevisionID\" = 1 "
	            + "      and ext_lvr.\"DeletionTimestamp\" = "
	            + "          timestamp '9999-12-31 00:00:00.0' "
	            + "where ar.\"Identifier\" = ? "
	            + "  and ar.\"EntityID\" = 1100 "
	            + "  and ar.\"RevisionID\" = 1 "
	            + "  and ar.\"DeletionTimestamp\" = "
	            + "      timestamp '9999-12-31 00:00:00.0'";

	    try (java.sql.PreparedStatement pstmnt =
	            connection().prepareStatement(sql)) {

	        pstmnt.setNString(1, identifier);
	        pstmnt.setQueryTimeout(30);

	        try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
	            if (rs.next()) {
	                Object currentStatus = rs.getObject("CurrentStatus");
	                Object previousStatus = rs.getObject("PreviousStatus");

	                result
	                    .put(
	                        "CurrentStatus",
	                        currentStatus == null
	                            ? ""
	                            : String.valueOf(currentStatus))
	                    .put(
	                        "PreviousStatus",
	                        previousStatus == null
	                            ? ""
	                            : String.valueOf(previousStatus))
	                    .put(
	                        "ExternalStatus",
	                        java.util.Objects.toString(
	                            rs.getString("ExternalStatus"),
	                            ""));
	            }
	        }
	    } catch (java.sql.SQLException e) {
	        logE(e);
	    }

	    return result;
	}
	
	public java.util.Map<String, org.json.JSONObject> getArticleValidityData(java.util.Collection<String> articleIdentifiers) throws java.sql.SQLException {

	    java.util.Map<String, org.json.JSONObject> result =
	            new java.util.HashMap<>();

	    if (articleIdentifiers == null || articleIdentifiers.isEmpty()) {
	        return result;
	    }

	    handleRefreshConnection();

	    java.util.List<String> ids =
	            articleIdentifiers.stream()
	                    .filter(java.util.Objects::nonNull)
	                    .filter(s -> !s.isBlank())
	                    .distinct()
	                    .collect(java.util.stream.Collectors.toList());

	    if (ids.isEmpty()) {
	        return result;
	    }

	    String placeholders =
	            String.join(
	                    ",",
	                    java.util.Collections.nCopies(ids.size(), "?"));

	    String sql =
	    		" select /*+ "
		        + "     leading(ar acv) "
		        + "     use_nl(acv) "
		        + "     index(ar IX_AR_TUNE_01) "
		        + "     index(acv IX_ACV_TUNE_02) "
		        + " */ "
	            + "        ar.\"Identifier\" \"ArticleIdentifier\" "
	            + "       ,max(case "
	            + "            when acv.\"CharacteristicID\" = 4383 "
	            + "            then acv.\"Value\" "
	            + "        end) \"FechaInicioVigenciaCostoNeto\" "
	            + "       ,max(case "
	            + "            when acv.\"CharacteristicID\" = 4382 "
	            + "            then acv.\"Value\" "
	            + "        end) \"FechaInicioVigenciaCostoImportacion\" "
	            + "       ,max(case "
	            + "            when acv.\"CharacteristicID\" = 4384 "
	            + "            then acv.\"Value\" "
	            + "        end) \"FechaInicioVigenciaPrecioVenta\" "
	            + " from \"ArticleRevision\" ar "
	            + " left join \"ArticleCharactValue\" acv "
	            + "    on acv.\"ArticleRevisionID\" = ar.\"ID\" "
	            + "   and acv.\"CharacteristicID\" in (4382,4383,4384) "
	            + "   and acv.\"DeletionTimestamp\" = "
	            + "       timestamp '9999-12-31 00:00:00.0' "
	            + " where ar.\"Identifier\" in (" + placeholders + ") "
	            + "   and ar.\"EntityID\" = 1000 "
	            + "   and ar.\"RevisionID\" = 1 "
	            + "   and ar.\"DeletionTimestamp\" = "
	            + "       timestamp '9999-12-31 00:00:00.0' "
	            + " group by ar.\"Identifier\"";

	    try (java.sql.PreparedStatement pstmnt =
	            connection().prepareStatement(sql)) {

	        int p = 1;

	        for (String id : ids) {
	            pstmnt.setNString(p++, id);
	        }

	        pstmnt.setQueryTimeout(5);
	        pstmnt.setFetchSize(ids.size());

	        try (java.sql.ResultSet rs = pstmnt.executeQuery()) {

	            while (rs.next()) {

	                String identifier =
	                        rs.getString("ArticleIdentifier");

	                result.put(
	                        identifier,
	                        new org.json.JSONObject()
	                                .put(
	                                        "FechaInicioVigenciaCostoNeto",
	                                        java.util.Objects.toString(
	                                                rs.getNString(
	                                                        "FechaInicioVigenciaCostoNeto"),
	                                                ""))
	                                .put(
	                                        "FechaInicioVigenciaCostoImportacion",
	                                        java.util.Objects.toString(
	                                                rs.getNString(
	                                                        "FechaInicioVigenciaCostoImportacion"),
	                                                ""))
	                                .put(
	                                        "FechaInicioVigenciaPrecioVenta",
	                                        java.util.Objects.toString(
	                                                rs.getNString(
	                                                        "FechaInicioVigenciaPrecioVenta"),
	                                                "")));
	            }
	        }
	    }

	    return result;
	}
	
	public java.util.List<org.json.JSONObject> getEccCharacteristicMetadataRows() {

		java.util.List<org.json.JSONObject> rows = new java.util.ArrayList<>();

		handleRefreshConnection();

		Integer eccSystemID = getLookupValueId("ExternalSystems", "ECC");

		if (eccSystemID == null) {
			log.log("No se encontró ECC en ExternalSystems.");
			return rows;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK2_CharacteristicRevision) "
				+ " */ "
				+ "        aa.\"Identifier\" "
				+ "       ,aa.\"DataType\" "
				+ "       ,bb.\"AlternativeIdentifier\" "
				+ "       ,aa.\"IsActive\" "
				+ "       ,case "
				+ "          when aa.\"IsActive\" = 1 "
				+ "           and instr("
				+ "                 ';' || replace(nvl(aa.\"Entities\", ''), ' ', '') || ';', "
				+ "                 ';Product2G;'"
				+ "               ) > 0 "
				+ "          then 1 else 0 "
				+ "        end \"IsProduct2G\" "
				+ "       ,case "
				+ "          when aa.\"IsActive\" = 1 "
				+ "           and instr("
				+ "                 ';' || replace(nvl(aa.\"Entities\", ''), ' ', '') || ';', "
				+ "                 ';Article;'"
				+ "               ) > 0 "
				+ "          then 1 else 0 "
				+ "        end \"IsArticle\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " inner join PIM_MAIN.\"CharacteristicIdentifier\" bb "
				+ "    on bb.\"CharacteristicRevisionID\" = aa.\"ID\" "
				+ "   and bb.\"SystemID\" = ? "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"AlternativeIdentifier\" is not null "
				+ " order by aa.\"Identifier\"";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setInt(1, eccSystemID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {

				while (rs.next()) {

					String characteristic =
							java.util.Objects.toString(
									rs.getString(1),
									"");

					String dataType =
							java.util.Objects.toString(
									rs.getString(2),
									"");

					String ecc =
							java.util.Objects.toString(
									rs.getString(3),
									"");

					if (characteristic.isEmpty() || ecc.isEmpty()) {
						continue;
					}

					rows.add(
						new org.json.JSONObject()
							.put("characteristic", characteristic)
							.put("dataType", dataType)
							.put("ecc", ecc)
							.put("active", rs.getInt(4) == 1)
							.put("product2G", rs.getInt(5) == 1)
							.put("article", rs.getInt(6) == 1)
					);
				}
			}

		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return rows;
	}
	
	public void collectLookupCharacteristics(
			java.util.Map<String, String> characteristicsInfo,
			java.util.Map<String, String> lkps) {

		handleRefreshConnection();

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK2_CharacteristicRevision) "
				+ "     index(bb XAK1_LookupRevision) "
				+ " */ "
				+ "        aa.\"Identifier\" "
				+ "       ,aa.\"DataType\" "
				+ "       ,bb.\"Identifier\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " left join PIM_MAIN.\"LookupRevision\" bb "
				+ "    on bb.\"LookupID\" = aa.\"LookupID\" "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"IsActive\" = 1 "
				+ "   and aa.\"Entities\" is not null "
				+ "   and aa.\"ParentCharacteristicID\" is null "
				+ "   and (aa.\"DataType\" <> 'NONE' or aa.\"DataType\" is null)";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getString(1);

					if (identifier == null || identifier.isBlank()) {
						continue;
					}

					characteristicsInfo.put(
							identifier,
							java.util.Objects.toString(rs.getString(2), ""));

					lkps.put(
							identifier,
							java.util.Objects.toString(rs.getString(3), ""));
				}
			}

		} catch (java.sql.SQLException e) {
			logE(e);
		}
	}
	
	public java.util.List<String> getActiveLookupCharacteristicIdentifiers() {
		java.util.List<String> values = new java.util.ArrayList<>();

		String sql =
				  " select /*+ qb_name(active_lookup_characteristics) */ "
				+ "        aa.\"Identifier\" "
				+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
				+ " where aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"DataType\" = 'LOOKUP' "
				+ "   and aa.\"IsActive\" = 1 "
				+ " order by aa.\"Identifier\" asc";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getString("Identifier");

					if (identifier != null && !identifier.isBlank()) {
						values.add(identifier);
					}
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return values;
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
				+ " where aa.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'Sitios Web' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
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

	private static final int BULK_BIND_CHUNK_SIZE = 900;

	private java.util.List<String> normalizeIdentifiers(
			java.util.Collection<String> identifiers) {

		java.util.LinkedHashSet<String> normalized =
				new java.util.LinkedHashSet<>();

		if (identifiers != null) {
			for (String identifier : identifiers) {
				if (identifier != null && !identifier.isBlank()) {
					normalized.add(identifier);
				}
			}
		}

		return new java.util.ArrayList<>(normalized);
	}

	private java.util.List<java.util.List<String>> identifierChunks(
			java.util.Collection<String> identifiers) {

		java.util.List<String> normalized = normalizeIdentifiers(identifiers);
		java.util.List<java.util.List<String>> chunks =
				new java.util.ArrayList<>();

		for (int start = 0;
				start < normalized.size();
				start += BULK_BIND_CHUNK_SIZE) {

			chunks.add(
				normalized.subList(
					start,
					Math.min(
						start + BULK_BIND_CHUNK_SIZE,
						normalized.size())));
		}

		return chunks;
	}

	private String placeholders(int count) {
		return String.join(
				",",
				java.util.Collections.nCopies(count, "?"));
	}

	private void bindNStrings(
			java.sql.PreparedStatement pstmnt,
			int firstParameter,
			java.util.List<String> values)
			throws java.sql.SQLException {

		for (int i = 0; i < values.size(); i++) {
			pstmnt.setNString(firstParameter + i, values.get(i));
		}
	}
	
	public java.util.Map<String, String> getProductsBySKUs(
			java.util.Collection<String> skus) {

		java.util.Map<String, String> productsBySKU =
				new java.util.LinkedHashMap<>();
		java.util.List<java.util.List<String>> chunks =
				identifierChunks(skus);

		if (chunks.isEmpty()) {
			return productsBySKU;
		}

		handleRefreshConnection();

		for (java.util.List<String> chunk : chunks) {
			java.util.List<Long> numericSKUs = new java.util.ArrayList<>();
			java.util.Map<Long, java.util.List<String>> originalSKUsByNumber =
					new java.util.LinkedHashMap<>();

			for (String sku : chunk) {
				try {
					Long numericSKU = Long.valueOf(sku);
					if (!originalSKUsByNumber.containsKey(numericSKU)) {
						numericSKUs.add(numericSKU);
						originalSKUsByNumber.put(
								numericSKU,
								new java.util.ArrayList<String>());
					}
					originalSKUsByNumber.get(numericSKU).add(sku);
				} catch (NumberFormatException e) {
					log("Invalid SKU: " + sku);
				}
			}

			if (numericSKUs.isEmpty()) {
				continue;
			}

			String productSql =
					  " select /*+ leading(aa bb) use_nl(bb) */ "
					+ "        aa.\"Res_Int_02\" \"SKU\" "
					+ "       ,bb.\"Identifier\" \"ProductNo\" "
					+ " from \"ArticleDetail\" aa "
					+ " inner join \"ArticleRevision\" bb "
					+ "    on bb.\"ID\" = aa.\"ArticleRevisionID\" "
					+ "   and bb.\"EntityID\" = 1100 "
					+ "   and bb.\"RevisionID\" = 1 "
					+ "   and bb.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Res_Int_02\" in ("
					+ placeholders(numericSKUs.size())
					+ ") "
					+ "   and aa.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(productSql)) {

				for (int i = 0; i < numericSKUs.size(); i++) {
					pstmnt.setLong(i + 1, numericSKUs.get(i));
				}

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						java.util.List<String> originalSKUs =
								originalSKUsByNumber.get(
										rs.getLong("SKU"));
						if (originalSKUs != null) {
							for (String originalSKU : originalSKUs) {
								productsBySKU.put(
										originalSKU,
										rs.getString("ProductNo"));
							}
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}

			String variantSql =
					  " select /*+ leading(aa bb) use_nl(bb cc dd) "
					+ "index(cc XAK1_ArticleReference) */ "
					+ "        aa.\"Res_Int_02\" \"SKU\" "
					+ "       ,dd.\"Identifier\" \"ProductNo\" "
					+ " from \"ArticleDetail\" aa "
					+ " inner join \"ArticleRevision\" bb "
					+ "    on bb.\"ID\" = aa.\"ArticleRevisionID\" "
					+ "   and bb.\"EntityID\" = 1000 "
					+ "   and bb.\"RevisionID\" = 1 "
					+ "   and bb.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " inner join \"ArticleReference\" cc "
					+ "    on cc.\"ArticleRevisionID\" = bb.\"ID\" "
					+ "   and cc.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " inner join \"ArticleRevision\" dd "
					+ "    on dd.\"ArticleID\" = cc.\"RefIntArtID\" "
					+ "   and dd.\"Identifier\" = cc.\"RefExtArtIdentifier\" "
					+ "   and dd.\"EntityID\" = 1100 "
					+ "   and dd.\"RevisionID\" = 1 "
					+ "   and dd.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Res_Int_02\" in ("
					+ placeholders(numericSKUs.size())
					+ ") "
					+ "   and aa.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(variantSql)) {

				for (int i = 0; i < numericSKUs.size(); i++) {
					pstmnt.setLong(i + 1, numericSKUs.get(i));
				}

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						java.util.List<String> originalSKUs =
								originalSKUsByNumber.get(
										rs.getLong("SKU"));
						if (originalSKUs != null) {
							for (String originalSKU : originalSKUs) {
								productsBySKU.putIfAbsent(
										originalSKU,
										rs.getString("ProductNo"));
							}
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return productsBySKU;
	}

	/**
	 * Resolves product/proposal identifiers in bulk. EntityID 1100 identifiers
	 * resolve to themselves; EntityID 1000 identifiers resolve through their
	 * active ArticleReference to the parent product.
	 */
	public java.util.Map<String, String> getProductsByIdentifiers(
			java.util.Collection<String> identifiers) {

		java.util.Map<String, String> productsByIdentifier =
				new java.util.LinkedHashMap<>();
		java.util.List<java.util.List<String>> chunks =
				identifierChunks(identifiers);

		if (chunks.isEmpty()) {
			return productsByIdentifier;
		}

		handleRefreshConnection();

		for (java.util.List<String> chunk : chunks) {
			String sql =
					  " select /*+ leading(aa) use_nl(bb) "
					+ "index(aa IX_AR_TUNE_01) "
					+ "index(bb XAK1_ArticleReference) */ "
					+ "        aa.\"Identifier\" \"RequestedIdentifier\" "
					+ "       ,case "
					+ "            when aa.\"EntityID\" = 1100 "
					+ "            then aa.\"Identifier\" "
					+ "            else bb.\"RefExtArtIdentifier\" "
					+ "        end \"ProductNo\" "
					+ " from \"ArticleRevision\" aa "
					+ " left join \"ArticleReference\" bb "
					+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "   and aa.\"EntityID\" = 1000 "
					+ "   and bb.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Identifier\" in ("
					+ placeholders(chunk.size())
					+ ") "
					+ "   and aa.\"EntityID\" in (1000, 1100) "
					+ "   and aa.\"RevisionID\" = 1 "
					+ "   and aa.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				bindNStrings(pstmnt, 1, chunk);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String productNo = rs.getString("ProductNo");
						if (productNo != null && !productNo.isBlank()) {
							productsByIdentifier.put(
									rs.getString("RequestedIdentifier"),
									productNo);
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return productsByIdentifier;
	}

	private org.json.JSONObject emptyProductData(String identifier) {
		return new org.json.JSONObject()
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
				.put("SupplierPartNumber", "");
	}
	
	public java.util.Map<String, org.json.JSONObject> getProductData(
			java.util.Collection<String> identifiers) {

		java.util.Map<String, org.json.JSONObject> result =
				new java.util.LinkedHashMap<>();
		java.util.List<java.util.List<String>> chunks =
				identifierChunks(identifiers);

		for (java.util.List<String> chunk : chunks) {
			for (String identifier : chunk) {
				result.put(identifier, emptyProductData(identifier));
			}

			String sql =
					  " SELECT /*+ leading(aa bb) "
					+ "use_nl(bb foto_cv assign_cv) "
					+ "index(aa IX_AR_TUNE_01) "
					+ "index(foto_cv XAK1_ArticleCharactValue) "
					+ "index(assign_cv XAK1_ArticleCharactValue) */ "
					+ "       aa.\"Identifier\" AS \"ProductNo\", "
					+ "       bb.\"EAN\", "
					+ "       bb.\"Res_Int_02\" AS \"SKU\", "
					+ "       bb.\"CurrentStatus\", "
					+ "       bus_lvr.\"Code\" AS \"Business\", "
					+ "       sec_lvr.\"Code\" AS \"Section\", "
					+ "       ig_lvr.\"Code\" AS \"ItemGroup\", "
					+ "       igs4h_lvr.\"Code\" AS \"ItemGroupS4H\", "
					+ "       brand_lvr.\"Code\" AS \"BrandName\", "
					+ "       brand_s4h_lvr.\"Code\" AS \"BRAND_ID_S4H\", "
					+ "       sap_lvr.\"Code\" AS \"SAPObjectType\", "
					+ "       sup_lvr.\"Code\" AS \"SupplierID\", "
					+ "       foto_lvr.\"Code\" AS \"FotoTomadaLiverpool\", "
					+ "       assign_lvr.\"Code\" AS \"AssignTakeNoTake\", "
					+ "       cc.\"Res_Text250_01\" AS \"SupplierPartNumber\", "
					+ "       dd.\"StructureGroupIdentifier\" "
					+ "FROM \"ArticleRevision\" aa "
					+ "INNER JOIN \"ArticleDetail\" bb "
					+ " ON bb.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND bb.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleDomain\" cc "
					+ " ON cc.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND cc.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleStructureMap\" dd "
					+ " ON dd.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND dd.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "AND dd.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'PrimaryProductTaxonomy' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
					+ "LEFT JOIN \"ArticleCharactValue\" foto_cv "
					+ " ON foto_cv.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND foto_cv.\"CharacteristicID\" = 4473 "
					+ "AND foto_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" foto_lvr "
					+ " ON foto_lvr.\"LookupValueID\" = foto_cv.\"LookupValueID\" "
					+ "AND foto_lvr.\"RevisionID\" = 1 "
					+ "AND foto_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleCharactValue\" assign_cv "
					+ " ON assign_cv.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND assign_cv.\"CharacteristicID\" = 3227 "
					+ "AND assign_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" assign_lvr "
					+ " ON assign_lvr.\"LookupValueID\" = assign_cv.\"LookupValueID\" "
					+ "AND assign_lvr.\"RevisionID\" = 1 "
					+ "AND assign_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" bus_lvr "
					+ " ON bus_lvr.\"LookupValueID\" = bb.\"Res_Int_01\" "
					+ "AND bus_lvr.\"RevisionID\" = 1 "
					+ "AND bus_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sec_lvr "
					+ " ON sec_lvr.\"LookupValueID\" = cc.\"Res_Int_02\" "
					+ "AND sec_lvr.\"RevisionID\" = 1 "
					+ "AND sec_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" ig_lvr "
					+ " ON ig_lvr.\"LookupValueID\" = cc.\"Res_Int_03\" "
					+ "AND ig_lvr.\"RevisionID\" = 1 "
					+ "AND ig_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" igs4h_lvr "
					+ " ON igs4h_lvr.\"LookupValueID\" = cc.\"Res_Int_04\" "
					+ "AND igs4h_lvr.\"RevisionID\" = 1 "
					+ "AND igs4h_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" brand_lvr "
					+ " ON brand_lvr.\"LookupValueID\" = cc.\"Res_Int_05\" "
					+ "AND brand_lvr.\"RevisionID\" = 1 "
					+ "AND brand_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" brand_s4h_lvr "
					+ " ON brand_s4h_lvr.\"LookupValueID\" = cc.\"Res_Int_06\" "
					+ "AND brand_s4h_lvr.\"RevisionID\" = 1 "
					+ "AND brand_s4h_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sap_lvr "
					+ " ON sap_lvr.\"LookupValueID\" = cc.\"Res_Int_08\" "
					+ "AND sap_lvr.\"RevisionID\" = 1 "
					+ "AND sap_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sup_lvr "
					+ " ON sup_lvr.\"LookupValueID\" = cc.\"Std_Int_10\" "
					+ "AND sup_lvr.\"RevisionID\" = 1 "
					+ "AND sup_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "WHERE aa.\"Identifier\" IN ("
					+ placeholders(chunk.size())
					+ ") "
					+ "AND aa.\"EntityID\" = 1100 "
					+ "AND aa.\"RevisionID\" = 1 "
					+ "AND aa.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				bindNStrings(pstmnt, 1, chunk);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String identifier = rs.getString("ProductNo");
						Object currentStatus = rs.getObject("CurrentStatus");
						result.put(
							identifier,
							emptyProductData(identifier)
								.put("Section", stringValue(rs, "Section"))
								.put("ItemGroup", stringValue(rs, "ItemGroup"))
								.put("ItemGroupS4H", stringValue(rs, "ItemGroupS4H"))
								.put("BrandName", stringValue(rs, "BrandName"))
								.put("BRAND_ID_S4H", stringValue(rs, "BRAND_ID_S4H"))
								.put("Business", stringValue(rs, "Business"))
								.put("SKU", stringValue(rs, "SKU"))
								.put("SupplierID", stringValue(rs, "SupplierID"))
								.put("Template", stringValue(rs, "StructureGroupIdentifier"))
								.put("CurrentStatus", currentStatus == null
										? ""
										: String.valueOf(currentStatus))
								.put("AssignTakeNoTake", stringValue(rs, "AssignTakeNoTake"))
								.put("SAPObjectType", stringValue(rs, "SAPObjectType"))
								.put("FotoTomadaLiverpool", stringValue(rs, "FotoTomadaLiverpool"))
								.put("MainBarCode", stringValue(rs, "EAN"))
								.put("SupplierPartNumber", stringValue(rs, "SupplierPartNumber")));
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}


	private String stringValue(
			java.sql.ResultSet rs,
			String column) throws java.sql.SQLException {

		return java.util.Objects.toString(rs.getString(column), "");
	}

	public java.util.Map<String, org.json.JSONObject> getProductExtraData(
			java.util.Collection<String> identifiers,
			String[] characteristicIdentifiers) {

		return getBulkExtraData(
				identifiers,
				characteristicIdentifiers,
				1100,
				"product",
				true);
	}

	public java.util.Map<String, org.json.JSONObject> getArticleExtraData(
			java.util.Collection<String> identifiers,
			String[] characteristicIdentifiers) {

		return getBulkExtraData(
				identifiers,
				characteristicIdentifiers,
				1000,
				"variant",
				false);
	}

	private java.util.Map<String, org.json.JSONObject> getBulkExtraData(
			java.util.Collection<String> identifiers,
			String[] characteristicIdentifiers,
			int entityID,
			String idProperty,
			boolean includeProductLanguageData) {

		java.util.Map<String, org.json.JSONObject> result =
				new java.util.LinkedHashMap<>();
		java.util.List<String> characteristics =
				normalizeIdentifiers(
					characteristicIdentifiers == null
						? java.util.Collections.emptyList()
						: java.util.Arrays.asList(characteristicIdentifiers));
		java.util.List<java.util.List<String>> idChunks =
				identifierChunks(identifiers);

		for (java.util.List<String> idChunk : idChunks) {
			for (String identifier : idChunk) {
				org.json.JSONObject row =
						new org.json.JSONObject().put(idProperty, identifier);
				for (String characteristic : characteristics) {
					row.put(characteristic, "");
				}
				if (includeProductLanguageData) {
					row.put("ProductName", "")
						.put("DescriptionLong", "")
						.put("DescriptionLong2", "");
				}
				result.put(identifier, row);
			}

			for (java.util.List<String> characteristicChunk :
					identifierChunks(characteristics)) {

				String sql =
						  " select /*+ leading(aa bb cc dd ee) "
						+ "use_nl(bb cc dd ee) "
						+ "index(aa IX_AR_TUNE_01) "
						+ "index(bb XAK1_ArticleCharactValue) "
						+ "index(cc XAK1_CharacteristicRevision) "
						+ "index(dd XAK1_LookupValueRevision) "
						+ "index(ee XAK1_LookupValueLang) */ "
						+ "        aa.\"Identifier\" \"ItemIdentifier\" "
						+ "       ,cc.\"Identifier\" \"CharacteristicIdentifier\" "
						+ "       ,case "
						+ "            when bb.\"Value\" is not null then bb.\"Value\" "
						+ "            else ee.\"Name\" "
						+ "        end \"CharacteristicValue\" "
						+ " from \"ArticleRevision\" aa "
						+ " inner join \"ArticleCharactValue\" bb "
						+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
						+ "   and bb.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " inner join PIM_MAIN.\"CharacteristicRevision\" cc "
						+ "    on cc.\"CharacteristicID\" = bb.\"CharacteristicID\" "
						+ "   and cc.\"RevisionID\" = 1 "
						+ "   and cc.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " left join PIM_MAIN.\"LookupValueRevision\" dd "
						+ "    on dd.\"LookupValueID\" = bb.\"LookupValueID\" "
						+ "   and dd.\"RevisionID\" = 1 "
						+ "   and dd.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " left join PIM_MAIN.\"LookupValueLang\" ee "
						+ "    on ee.\"LookupValueRevisionID\" = dd.\"ID\" "
						+ "   and ee.\"LanguageID\" = 10 "
						+ "   and ee.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " where aa.\"Identifier\" in ("
						+ placeholders(idChunk.size())
						+ ") "
						+ "   and cc.\"Identifier\" in ("
						+ placeholders(characteristicChunk.size())
						+ ") "
						+ "   and aa.\"EntityID\" = ? "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0'";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(sql)) {

					bindNStrings(pstmnt, 1, idChunk);
					bindNStrings(
							pstmnt,
							idChunk.size() + 1,
							characteristicChunk);
					pstmnt.setInt(
							idChunk.size()
								+ characteristicChunk.size()
								+ 1,
							entityID);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						while (rs.next()) {
							org.json.JSONObject row =
									result.get(rs.getString("ItemIdentifier"));
							if (row != null) {
								row.put(
									rs.getString("CharacteristicIdentifier"),
									java.util.Objects.toString(
										rs.getNString("CharacteristicValue"),
										""));
							}
						}
					}
				} catch (java.sql.SQLException e) {
					logE(e);
				}
			}

			if (includeProductLanguageData) {
				String languageSql =
						  " select /*+ leading(aa bb) use_nl(bb) "
						+ "index(aa IX_AR_TUNE_01) "
						+ "index(bb XAK1_ArticleLang) */ "
						+ "        aa.\"Identifier\" \"ProductNo\" "
						+ "       ,bb.\"Res_Text250_01\" \"ProductName\" "
						+ "       ,bb.\"DescriptionLong\" \"DescriptionLong\" "
						+ "       ,bb.\"Res_Text2G_01\" \"DescriptionLong2\" "
						+ " from \"ArticleRevision\" aa "
						+ " inner join \"ArticleLang\" bb "
						+ "    on bb.\"ArticleRevisionID\" = aa.\"ID\" "
						+ "   and bb.\"LanguageID\" = 10 "
						+ "   and bb.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0' "
						+ " where aa.\"Identifier\" in ("
						+ placeholders(idChunk.size())
						+ ") "
						+ "   and aa.\"EntityID\" = 1100 "
						+ "   and aa.\"RevisionID\" = 1 "
						+ "   and aa.\"DeletionTimestamp\" = "
						+ "       timestamp '9999-12-31 00:00:00.0'";

				try (java.sql.PreparedStatement pstmnt =
						connection().prepareStatement(languageSql)) {

					bindNStrings(pstmnt, 1, idChunk);

					try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
						while (rs.next()) {
							org.json.JSONObject row =
									result.get(rs.getString("ProductNo"));
							if (row != null) {
								row.put("ProductName", stringValue(rs, "ProductName"))
									.put("DescriptionLong", stringValue(rs, "DescriptionLong"))
									.put("DescriptionLong2", stringValue(rs, "DescriptionLong2"));
							}
						}
					}
				} catch (java.sql.SQLException e) {
					logE(e);
				}
			}
		}

		return result;
	}

	public java.util.Map<String, java.util.Set<String>> getProductVariants(
			java.util.Collection<String> identifiers) {

		java.util.Map<String, java.util.Set<String>> result =
				new java.util.LinkedHashMap<>();

		for (java.util.List<String> chunk : identifierChunks(identifiers)) {
			for (String identifier : chunk) {
				result.put(
						identifier,
						new java.util.LinkedHashSet<String>());
			}

			String sql =
					  " select /*+ leading(aa bb) use_nl(bb cc) "
					+ "index(bb XAK1_ArticleReference) */ "
					+ "        aa.\"Identifier\" \"ProductNo\" "
					+ "       ,cc.\"Identifier\" \"ArticleIdentifier\" "
					+ " from \"ArticleRevision\" aa "
					+ " inner join \"ArticleReference\" bb "
					+ "    on bb.\"RefIntArtID\" = aa.\"ArticleID\" "
					+ "   and bb.\"RefExtArtIdentifier\" = aa.\"Identifier\" "
					+ "   and bb.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " inner join \"ArticleRevision\" cc "
					+ "    on cc.\"ID\" = bb.\"ArticleRevisionID\" "
					+ "   and cc.\"EntityID\" = 1000 "
					+ "   and cc.\"RevisionID\" = 1 "
					+ "   and cc.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Identifier\" in ("
					+ placeholders(chunk.size())
					+ ") "
					+ "   and aa.\"EntityID\" = 1100 "
					+ "   and aa.\"RevisionID\" = 1 "
					+ "   and aa.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " order by aa.\"Identifier\", cc.\"Identifier\"";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				bindNStrings(pstmnt, 1, chunk);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						java.util.Set<String> variants =
								result.get(rs.getString("ProductNo"));
						if (variants != null) {
							variants.add(rs.getString("ArticleIdentifier"));
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}

	public java.util.Map<String, org.json.JSONObject> getArticleData(
			java.util.Collection<String> identifiers) {

		java.util.Map<String, org.json.JSONObject> result =
				new java.util.LinkedHashMap<>();

		for (java.util.List<String> chunk : identifierChunks(identifiers)) {
			for (String identifier : chunk) {
				result.put(identifier, emptyArticleData(identifier));
			}

			String sql =
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
					+ " ON bb.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND bb.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleDomain\" cc "
					+ " ON cc.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND cc.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleCharactValue\" assign_cv "
					+ " ON assign_cv.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND assign_cv.\"CharacteristicID\" = 3227 "
					+ "AND assign_cv.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" assign_lvr "
					+ " ON assign_lvr.\"LookupValueID\" = assign_cv.\"LookupValueID\" "
					+ "AND assign_lvr.\"RevisionID\" = 1 "
					+ "AND assign_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" sec_lvr "
					+ " ON sec_lvr.\"LookupValueID\" = cc.\"Res_Int_01\" "
					+ "AND sec_lvr.\"RevisionID\" = 1 "
					+ "AND sec_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN PIM_MAIN.\"LookupValueRevision\" ig_lvr "
					+ " ON ig_lvr.\"LookupValueID\" = cc.\"Res_Int_02\" "
					+ "AND ig_lvr.\"RevisionID\" = 1 "
					+ "AND ig_lvr.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "LEFT JOIN \"ArticleReference\" ar "
					+ " ON ar.\"ArticleRevisionID\" = aa.\"ID\" "
					+ "AND ar.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0' "
					+ "WHERE aa.\"Identifier\" IN ("
					+ placeholders(chunk.size())
					+ ") "
					+ "AND aa.\"EntityID\" = 1000 "
					+ "AND aa.\"RevisionID\" = 1 "
					+ "AND aa.\"DeletionTimestamp\" = TIMESTAMP '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
				bindNStrings(pstmnt, 1, chunk);
				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String identifier = rs.getString("VariantIdentifier");
						result.put(
							identifier,
							emptyArticleData(identifier)
								.put("ProductNo", stringValue(rs, "ProductNo"))
								.put("SKU", stringValue(rs, "SKU"))
								.put("ColoursLiverpoolAtt", stringValue(rs, "Color"))
								.put("TamanoUnico", stringValue(rs, "TamanoUnico"))
								.put("ProductImage", stringValue(rs, "ProductImageURL"))
								.put("MainBarCode", stringValue(rs, "EAN"))
								.put("AssignTakeNoTake", stringValue(rs, "AssignTakeNoTake"))
								.put("SupplierPartNumber", stringValue(rs, "SupplierPartNumber")));
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}


	private org.json.JSONObject emptyArticleData(String identifier) {
		return new org.json.JSONObject()
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
				+ " where aa.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'Sitios Web' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
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
				+ "   and aa.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'PrimaryProductTaxonomy' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
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
	
	public String getPartySupplierType(String supplierCode) {
		if (supplierCode == null || supplierCode.isBlank()) {
			return null;
		}

		java.util.Map<String, String> supplierTypes =
				getReferencedLookupValues(
						"Party",
						supplierCode,
						"TipoProveedorSAPAttLOV",
						10);

		return supplierTypes.isEmpty()
				? null
				: supplierTypes.keySet().iterator().next();
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
	
	public Integer getProductCurrentStatusByArticleIdentifier(String articleIdentifier) {
		handleRefreshConnection();

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(
				  "select /*+ leading(article_ar article_ref product_ar product_ad) "
				+ "           use_nl(article_ref product_ar product_ad) "
				+ "           index(article_ar IX_AR_TUNE_01) "
				+ "           index(article_ref XAK1_ArticleReference) "
				+ "           index(product_ar IX_AR_TUNE_01) */ "
				+ "       product_ad.\"CurrentStatus\" "
				+ "from PIM_MASTER.\"ArticleRevision\" article_ar "
				+ "inner join PIM_MASTER.\"ArticleReference\" article_ref "
				+ "        on article_ref.\"ArticleRevisionID\" = article_ar.\"ID\" "
				+ "       and article_ref.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "inner join PIM_MASTER.\"ArticleRevision\" product_ar "
				+ "        on product_ar.\"ArticleID\" = article_ref.\"RefIntArtID\" "
				+ "       and product_ar.\"Identifier\" = article_ref.\"RefExtArtIdentifier\" "
				+ "       and product_ar.\"EntityID\" = 1100 "
				+ "       and product_ar.\"RevisionID\" = 1 "
				+ "       and product_ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "inner join PIM_MASTER.\"ArticleDetail\" product_ad "
				+ "        on product_ad.\"ArticleRevisionID\" = product_ar.\"ID\" "
				+ "       and product_ad.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "where article_ar.\"Identifier\" = ? "
				+ "  and article_ar.\"EntityID\" = 1000 "
				+ "  and article_ar.\"RevisionID\" = 1 "
				+ "  and article_ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'"
		)) {
			pstmnt.setString(1, articleIdentifier);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					int currentStatus = rs.getInt("CurrentStatus");
					return rs.wasNull() ? null : Integer.valueOf(currentStatus);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return null;
	}
	

	public java.util.Map<String, org.json.JSONObject> getTemplateCharacteristicPropertiesForVendorCenter(String template) {
	
		java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
	
		if (template == null || template.isBlank()) {
			return result;
		}
	
		Integer templateLVID = getLookupValueId("PPH_L4_Templates", template);
		Integer euCatSystemID = getLookupValueId("ExternalSystems", "EUCat");
	
		if (templateLVID == null || euCatSystemID == null) {
			return result;
		}
	
		String sql =
				  " select /*+ "
				+ "     leading(bb aa dd pp pi cl lr) "
				+ "     use_nl(aa dd pp pi cl lr) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa IX_LVREV_METADATA_EXT_01) "
				+ "     index(dd XAK1_CharacteristicRevision) "
				+ "     index(pp XAK1_LookupValueRevision) "
				+ "     index(pi XAK1_LookupValueIdentifier) "
				+ "     index(cl XAK1_CharacteristicLang) "
				+ "     index(lr XAK1_LookupRevision) "
				+ " */ "
				+ "        dd.\"Identifier\" \"Characteristic\" "
				+ "       ,pi.\"Code\" \"Property\" "
				+ "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
				+ "       ,cl.\"Name\" \"FriendlyName\" "
				+ "       ,dd.\"DataType\" \"DataType\" "
				+ "       ,lr.\"Identifier\" \"CharacteristicLookup\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"Res_Int_01\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" dd "
				+ "    on dd.\"CharacteristicID\" = aa.\"Res_Int_02\" "
				+ "   and dd.\"RevisionID\" = 1 "
				+ "   and dd.\"IsActive\" = 1 "
				+ "   and dd.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" pp "
				+ "    on pp.\"LookupValueID\" = aa.\"Res_Int_03\" "
				+ "   and pp.\"RevisionID\" = 1 "
				+ "   and pp.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueIdentifier\" pi "
				+ "    on pi.\"LookupValueRevisionID\" = pp.\"ID\" "
				+ "   and pi.\"SystemID\" = ? "
				+ "   and pi.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"CharacteristicLang\" cl "
				+ "    on cl.\"CharacteristicRevisionID\" = dd.\"ID\" "
				+ "   and cl.\"LanguageID\" = 10 "
				+ "   and cl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupRevision\" lr "
				+ "    on lr.\"LookupID\" = dd.\"LookupID\" "
				+ "   and lr.\"RevisionID\" = 1 "
				+ "   and lr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where bb.\"Identifier\" = "
				+ "       'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by dd.\"Identifier\" asc, pi.\"Code\" asc";
	
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
	
			pstmnt.setInt(1, templateLVID);
			pstmnt.setInt(2, euCatSystemID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);
	
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String characteristic =
							java.util.Objects.toString(
									rs.getNString("Characteristic"), "");
	
					String property =
							java.util.Objects.toString(
									rs.getNString("Property"), "");
	
					if (characteristic.isBlank() || property.isBlank()) {
						continue;
					}
	
					org.json.JSONObject values = result.get(characteristic);
	
					if (values == null) {
						values = new org.json.JSONObject()
								.put(
										"_friendlyName",
										java.util.Objects.toString(
												rs.getNString("FriendlyName"),
												characteristic))
								.put(
										"_dataType",
										java.util.Objects.toString(
												rs.getNString("DataType"), ""))
								.put(
										"_lookup",
										java.util.Objects.toString(
												rs.getNString(
														"CharacteristicLookup"),
												""));
	
						result.put(characteristic, values);
					}
	
					values.put(
							property,
							java.util.Objects.toString(
									rs.getNString("PropertyValue"), ""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
	
		return result;
	}
	
	public java.util.List<org.json.JSONObject> getTemplateCharacteristicPropertyRowsByLocalizedName(
				String template,
				String creationType,
				int languageID) {
	
		java.util.List<org.json.JSONObject> rows =
				new java.util.ArrayList<>();
		
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
				+ "     leading(bb aa sg cr cl pv pl) "
				+ "     use_nl(aa sg cr cl pv pl) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa IX_LVREV_METADATA_EXT_01) "
				+ "     index(sg XAK1_LookupValueRevision) "
				+ "     index(cr XAK1_CharacteristicRevision) "
				+ "     index(cl XAK1_CharacteristicLang) "
				+ "     index(pv XAK1_LookupValueRevision) "
				+ "     index(pl XAK1_LookupValueLang) "
				+ " */ "
				+ "        sg.\"Code\" \"StructureGroup\" "
				+ "       ,cr.\"Identifier\" \"CharacteristicIdentifier\" "
				+ "       ,cl.\"Name\" \"CharacteristicName\" "
				+ "       ,pl.\"Name\" \"Property\" "
				+ "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"Res_Int_01\" = ? "
				+ "   and aa.\"Res_Int_04\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" sg "
				+ "    on sg.\"LookupValueID\" = aa.\"Res_Int_01\" "
				+ "   and sg.\"RevisionID\" = 1 "
				+ "   and sg.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cr "
				+ "    on cr.\"CharacteristicID\" = aa.\"Res_Int_02\" "
				+ "   and cr.\"RevisionID\" = 1 "
				+ "   and cr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"CharacteristicLang\" cl "
				+ "    on cl.\"CharacteristicRevisionID\" = cr.\"ID\" "
				+ "   and cl.\"LanguageID\" = ? "
				+ "   and cl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" pv "
				+ "    on pv.\"LookupValueID\" = aa.\"Res_Int_03\" "
				+ "   and pv.\"RevisionID\" = 1 "
				+ "   and pv.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" pl "
				+ "    on pl.\"LookupValueRevisionID\" = pv.\"ID\" "
				+ "   and pl.\"LanguageID\" = ? "
				+ "   and pl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where bb.\"Identifier\" = "
				+ "       'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by cl.\"Name\" asc, cr.\"Identifier\" asc, "
				+ "          pl.\"Name\" asc";
		
		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
		
			pstmnt.setInt(1, templateLVID);
			pstmnt.setInt(2, creationTypeLVID);
			pstmnt.setInt(3, languageID);
			pstmnt.setInt(4, languageID);
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(500);
		
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					rows.add(
							new org.json.JSONObject()
									.put(
											"structureGroup",
											java.util.Objects.toString(
													rs.getNString("StructureGroup"),
													template))
									.put(
											"characteristicIdentifier",
											java.util.Objects.toString(
													rs.getNString(
															"CharacteristicIdentifier"),
													""))
									.put(
											"characteristicName",
											java.util.Objects.toString(
													rs.getNString("CharacteristicName"),
													""))
									.put(
											"property",
											java.util.Objects.toString(
													rs.getNString("Property"), ""))
									.put(
											"propertyValue",
											java.util.Objects.toString(
													rs.getNString("PropertyValue"),
													"")));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		
		return rows;
	}
	
	public java.util.Date getTemplateCharacteristicMetadataLastChangeDate( String template ) {

		if (template == null || template.isBlank()) {
			return null;
		}

		Integer templateLVID = getLookupValueId("PPH_L4_Templates", template);

		if (templateLVID == null) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     leading(bb aa cr) "
				+ "     use_nl(aa cr) "
				+ "     index(bb XAK2_LookupRevision) "
				+ "     index(aa IX_LVREV_METADATA_EXT_01) "
				+ "     index(cr XAK1_CharacteristicRevision) "
				+ " */ "
				+ "        max(nvl( "
				+ "            aa.\"ModificationTimestamp\", "
				+ "            aa.\"CreationTimestamp\" "
				+ "        )) \"LastChange\" "
				+ " from PIM_MAIN.\"LookupRevision\" bb "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"Res_Int_01\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cr "
				+ "    on cr.\"CharacteristicID\" = aa.\"Res_Int_02\" "
				+ "   and cr.\"RevisionID\" = 1 "
				+ "   and cr.\"IsActive\" = 1 "
				+ "   and cr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where bb.\"Identifier\" = "
				+ "       'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0'";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {

			pstmnt.setInt(1, templateLVID);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					java.sql.Timestamp value = rs.getTimestamp("LastChange");
					return value == null
							? null
							: new java.util.Date(value.getTime());
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return null;
	}
	

    /**
     * Devuelve la configuración local VendorCenterSection por característica.
     * Si existen varias entradas locales para la misma característica, conserva
     * la más recientemente modificada, igual que el put() sucesivo que hacía el
     * consumidor REST anterior.
     */
    public java.util.Map<String, String> getVendorCenterSectionOverrides() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        Integer euCatSystemID = getLookupValueId("ExternalSystems", "EUCat");
        if (euCatSystemID == null) {
            return values;
        }

        String sql =
                  " select /*+ "
                + "     leading(bb aa cr pv pvi) "
                + "     use_nl(aa cr pv pvi) "
                + "     index(bb XAK2_LookupRevision) "
                + "     index(aa IX_LVREV_METADATA_EXT_01) "
                + "     index(cr XAK1_CharacteristicRevision) "
                + "     index(pv XAK1_LookupValueRevision) "
                + "     index(pvi XAK1_LookupValueIdentifier) "
                + " */ "
                + "        cr.\"Identifier\" \"Characteristic\" "
                + "       ,aa.\"Res_Text2G_01\" \"PropertyValue\" "
                + " from PIM_MAIN.\"LookupRevision\" bb "
                + " inner join PIM_MAIN.\"LookupValueRevision\" aa "
                + "    on aa.\"LookupID\" = bb.\"LookupID\" "
                + "   and aa.\"RevisionID\" = 1 "
                + "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                + " inner join PIM_MAIN.\"CharacteristicRevision\" cr "
                + "    on cr.\"CharacteristicID\" = aa.\"Res_Int_02\" "
                + "   and cr.\"RevisionID\" = 1 "
                + "   and cr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                + " inner join PIM_MAIN.\"LookupValueRevision\" pv "
                + "    on pv.\"LookupValueID\" = aa.\"Res_Int_03\" "
                + "   and pv.\"RevisionID\" = 1 "
                + "   and pv.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                + " inner join PIM_MAIN.\"LookupValueIdentifier\" pvi "
                + "    on pvi.\"LookupValueRevisionID\" = pv.\"ID\" "
                + "   and pvi.\"SystemID\" = ? "
                + "   and pvi.\"Code\" = 'VendorCenterSection' "
                + "   and pvi.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                + " where bb.\"Identifier\" = 'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla' "
                + "   and bb.\"RevisionID\" = 1 "
                + "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                + "   and aa.\"Res_Text2G_01\" is not null "
                + " order by cr.\"Identifier\" asc, "
                + "          nvl(aa.\"ModificationTimestamp\", aa.\"CreationTimestamp\") asc, "
                + "          aa.\"ID\" asc";

        try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
            pstmnt.setInt(1, euCatSystemID);
            pstmnt.setQueryTimeout(30);
            pstmnt.setFetchSize(500);
            try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
                while (rs.next()) {
                    String characteristic = rs.getString("Characteristic");
                    String value = rs.getNString("PropertyValue");
                    if (characteristic != null && !characteristic.isBlank()
                            && value != null && !value.isBlank()) {
                        values.put(characteristic, value);
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            logE(e);
        }

        return values;
    }

	public java.util.Date getDictionaryLastChangeDate( String dictionaryIdentifier ) {

		if (dictionaryIdentifier == null || dictionaryIdentifier.isBlank()) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     leading(aa bb) "
				+ "     use_nl(bb) "
				+ "     index(aa XAK1_Dictionary) "
				+ "     index(bb IX_DICTENTRY_TUNE_02) "
				+ " */ "
				+ "        max(nvl( "
				+ "            bb.\"ModificationTimestamp\", "
				+ "            bb.\"CreationTimestamp\" "
				+ "        )) \"LastChange\" "
				+ " from PIM_MAIN.\"Dictionary\" aa "
				+ " inner join PIM_MAIN.\"DictionaryEntry\" bb "
				+ "    on bb.\"DictionaryID\" = aa.\"ID\" "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0'";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, dictionaryIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					java.sql.Timestamp value = rs.getTimestamp("LastChange");
					return value == null
							? null
							: new java.util.Date(value.getTime());
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return null;
	}
	
	public java.util.Date getStructureGroupLastModified(String template) {
		if (template == null || template.isBlank()) {
			return null;
		}

		String sql =
				  " select /*+ "
				+ "     index(aa XAK1_StructureGroupRevision) "
				+ "     first_rows(1) "
				+ " */ "
				+ "        nvl( "
				+ "            aa.\"LastModified\", "
				+ "            nvl( "
				+ "                aa.\"ModificationTimestamp\", "
				+ "                aa.\"CreationTimestamp\" "
				+ "            ) "
				+ "        ) \"LastModified\" "
				+ " from PIM_MAIN.\"StructureGroupRevision\" aa "
				+ " where aa.\"Identifier\" = ? "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'PrimaryProductTaxonomy' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and rownum = 1";

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, template);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				if (rs.next()) {
					java.sql.Timestamp value =
							rs.getTimestamp("LastModified");

					return value == null
							? null
							: new java.util.Date(value.getTime());
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return null;
	}
	
	public Integer getLookupValueId(String lookupIdentifier, String code, boolean failOnError) {
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
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"Code\" = ? "
				+ " where bb.\"Identifier\" = ? "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0'";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, code);
			pstmnt.setNString(2, lookupIdentifier);
			pstmnt.setQueryTimeout(30);
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			if (failOnError) {
				throw new IllegalStateException( "Error consultando el código " + code + " en el lookup " + lookupIdentifier, e );
			}
			return null;
		}
	}
	
	public java.util.List<String> getLookupValueCodesByName(
			String lookupIdentifier,
			int languageID,
			String name,
			boolean onlyActive) {

		java.util.List<String> codes = new java.util.ArrayList<>();

		if (lookupIdentifier == null
				|| lookupIdentifier.isBlank()
				|| name == null
				|| name.isBlank()) {
			return codes;
		}

		String sql =
				  " select /*+ leading(cc bb aa) use_nl(bb aa) */ "
				+ "        bb.\"Code\" "
				+ " from PIM_MAIN.\"LookupValueLang\" cc "
				+ " inner join PIM_MAIN.\"LookupValueRevision\" bb "
				+ "    on bb.\"ID\" = cc.\"LookupValueRevisionID\" "
				+ "   and bb.\"RevisionID\" = 1 "
				+ "   and bb.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ (onlyActive ? "   and bb.\"IsActive\" = 1 " : "")
				+ " inner join PIM_MAIN.\"LookupRevision\" aa "
				+ "    on aa.\"LookupID\" = bb.\"LookupID\" "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where cc.\"Name\" = ? "
				+ "   and cc.\"LanguageID\" = ? "
				+ "   and cc.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"Identifier\" = ?";

		try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
			pstmnt.setNString(1, name);
			pstmnt.setInt(2, languageID);
			pstmnt.setNString(3, lookupIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String code = rs.getString(1);
					if (code != null && !code.isBlank()) {
						codes.add(code);
					}
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return codes;
	}

	public java.util.Map<String, String> getTemplateStructureGroupAttributeValues(
				String template,
				int languageID,
				java.util.Collection<String> attributeNames) {
	
		java.util.Map<String, String> values = new java.util.TreeMap<>();
		
		if (template == null
				|| template.isBlank()
				|| attributeNames == null
				|| attributeNames.isEmpty()) {
			return values;
		}
		
		java.util.List<String> requestedNames = new java.util.ArrayList<>();
		
		for (String attributeName : attributeNames) {
			if (attributeName != null && !attributeName.isBlank()) {
				requestedNames.add(attributeName.trim());
			}
		}
		
		if (requestedNames.isEmpty()) {
			return values;
		}
	
		String placeholders = String.join(
				",",
				java.util.Collections.nCopies(requestedNames.size(), "?"));
		
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
				+ "        ee.\"Name\" \"AttributeName\" "
				+ "       ,cc.\"Value\" \"AttributeValue\" "
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
				+ "   and aa.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'PrimaryProductTaxonomy' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') "
				+ "   and aa.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ "   and ee.\"Name\" in (" + placeholders + ") "
				+ " order by ee.\"Name\" asc";
		
		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {
		
			int parameterIndex = 1;
			pstmnt.setInt(parameterIndex++, languageID);
			pstmnt.setInt(parameterIndex++, languageID);
			pstmnt.setNString(parameterIndex++, template);
		
			for (String attributeName : requestedNames) {
				pstmnt.setNString(parameterIndex++, attributeName);
			}
		
			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(Math.min(requestedNames.size(), 500));
		
			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String attributeName =
							java.util.Objects.toString(
									rs.getNString("AttributeName"), "");
		
					if (attributeName.isBlank()) {
						continue;
					}
		
					values.put(
							attributeName,
							java.util.Objects.toString(
									rs.getNString("AttributeValue"), ""));
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
		
		return values;
	}
	
	
	public java.util.Map<String, java.util.Map<String, String>> getReferencedLookupValuesBySourceCodes(
				String sourceLookupIdentifier,
				java.util.Collection<String> sourceValueCodes,
				String targetLookupIdentifier,
				int languageID) {
		java.util.Map<String, java.util.Map<String, String>> result = new java.util.LinkedHashMap<>();
		java.util.List<String> normalizedSourceCodes = normalizeIdentifiers(sourceValueCodes);
		if (sourceLookupIdentifier == null
				|| sourceLookupIdentifier.isBlank()
				|| normalizedSourceCodes.isEmpty()
				|| targetLookupIdentifier == null
				|| targetLookupIdentifier.isBlank()) {
		
			return result;
		}
		
		for (String sourceValueCode : normalizedSourceCodes) {
			result.put(sourceValueCode, new java.util.LinkedHashMap<String, String>());
		}
		
		for (java.util.List<String> chunk : identifierChunks(normalizedSourceCodes)) {
			String sql =
					  "select /*+ "
					+ "           qb_name(main_qb) "
					+ "           leading(src_lookup src_value ref target_lookup target_value target_lang) "
					+ "           use_nl(src_value ref target_lookup target_value target_lang) "
					+ "           index(src_lookup XAK2_LookupRevision) "
					+ "           index(src_value XAK2_LookupValueRevision) "
					+ "           index(ref XAK1_LookupValueReference) "
					+ "           index(target_lookup XAK2_LookupRevision) "
					+ "           index(target_value XAK1_LookupValueRevision) "
					+ "       */ "
					+ "       src_value.\"Code\" "
					+ "           \"SourceCode\", "
					+ "       target_value.\"Code\" "
					+ "           \"ReferencedCode\", "
					+ "       target_lang.\"Name\" "
					+ "           \"ReferencedName\" "
					+ "from PIM_MAIN.\"LookupRevision\" src_lookup "
					+ "inner join PIM_MAIN.\"LookupValueRevision\" src_value "
					+ "        on src_value.\"LookupID\" = src_lookup.\"LookupID\" "
					+ "       and src_value.\"Code\" in ("
					+ placeholders(chunk.size())
					+ ") "
					+ "       and src_value.\"RevisionID\" = 1 "
					+ "       and src_value.\"DeletionTimestamp\" = "
					+ "           timestamp '9999-12-31 00:00:00.0' "
					+ "inner join PIM_MAIN.\"LookupValueReference\" ref "
					+ "        on ref.\"LookupValueRevisionID\" = src_value.\"ID\" "
					+ "       and ref.\"DeletionTimestamp\" = "
					+ "           timestamp '9999-12-31 00:00:00.0' "
					+ "inner join PIM_MAIN.\"LookupRevision\" target_lookup "
					+ "        on target_lookup.\"LookupID\" = ref.\"RefLookupID\" "
					+ "       and target_lookup.\"Identifier\" = ? "
					+ "       and target_lookup.\"RevisionID\" = 1 "
					+ "       and target_lookup.\"DeletionTimestamp\" = "
					+ "           timestamp '9999-12-31 00:00:00.0' "
					+ "inner join PIM_MAIN.\"LookupValueRevision\" target_value "
					+ "        on target_value.\"LookupValueID\" = ref.\"RefLookupValueID\" "
					+ "       and target_value.\"LookupID\" = ref.\"RefLookupID\" "
					+ "       and target_value.\"RevisionID\" = 1 "
					+ "       and target_value.\"DeletionTimestamp\" = "
					+ "           timestamp '9999-12-31 00:00:00.0' "
					+ "left join PIM_MAIN.\"LookupValueLang\" target_lang "
					+ "       on target_lang.\"LookupValueRevisionID\" = target_value.\"ID\" "
					+ "      and target_lang.\"LanguageID\" = ? "
					+ "      and target_lang.\"DeletionTimestamp\" = "
					+ "          timestamp '9999-12-31 00:00:00.0' "
					+ "where src_lookup.\"Identifier\" = ? "
					+ "  and src_lookup.\"RevisionID\" = 1 "
					+ "  and src_lookup.\"DeletionTimestamp\" = "
					+ "      timestamp '9999-12-31 00:00:00.0' "
					+ "order by src_value.\"Code\", target_value.\"Code\"";
		
			try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
				bindNStrings(
						pstmnt,
						1,
						chunk);
				int parameterIndex = chunk.size() + 1;
		
				pstmnt.setNString( parameterIndex++, targetLookupIdentifier );
				pstmnt.setInt(parameterIndex++, languageID);
				pstmnt.setNString( parameterIndex, sourceLookupIdentifier );
		
				pstmnt.setQueryTimeout(30);
				pstmnt.setFetchSize(500);
		
				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String sourceCode = rs.getString("SourceCode");
						String referencedCode = rs.getString("ReferencedCode");
						if (sourceCode == null
								|| sourceCode.isBlank()
								|| referencedCode == null
								|| referencedCode.isBlank()) {
							continue;
						}
		
						java.util.Map<String, String> referencedValues = result.get(sourceCode);
		
						if (referencedValues == null) {
							referencedValues = new java.util.LinkedHashMap<>();
							result.put(sourceCode, referencedValues);
						}
						referencedValues.put( referencedCode, java.util.Objects.toString( rs.getString("ReferencedName"), "") );
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}
		
		return result;
	}
	
	public java.util.Map<String, String> getReferencedLookupValues(
			String sourceLookupIdentifier,
			String sourceValueCode,
			String targetLookupIdentifier,
			int languageID
	) {
		java.util.Map<String, String> values = new java.util.LinkedHashMap<>();

		if (sourceLookupIdentifier == null || sourceLookupIdentifier.isBlank()
				|| sourceValueCode == null || sourceValueCode.isBlank()
				|| targetLookupIdentifier == null || targetLookupIdentifier.isBlank()) {
			return values;
		}

		String sql =
				    "select /*+\r\n"
				  + "           qb_name(main_qb)\r\n"
				  + "           leading(src_lookup src_value ref target_lookup target_value target_lang)\r\n"
				  + "           use_nl(src_value ref target_lookup target_value target_lang)\r\n"
				  + "           index(src_lookup XAK2_LookupRevision)\r\n"
				  + "           index(src_value XAK2_LookupValueRevision)\r\n"
				  + "           index(ref XAK1_LookupValueReference)\r\n"
				  + "           index(target_lookup XAK2_LookupRevision)\r\n"
				  + "           index(target_value XAK1_LookupValueRevision)\r\n"
				  + "       */\r\n"
				  + "       target_value.\"Code\" \"ReferencedCode\",\r\n"
				  + "       target_lang.\"Name\" \"ReferencedName\"\r\n"
				  + "from PIM_MAIN.\"LookupRevision\" src_lookup\r\n"
				  + "inner join PIM_MAIN.\"LookupValueRevision\" src_value\r\n"
				  + "        on src_value.\"LookupID\" = src_lookup.\"LookupID\"\r\n"
				  + "       and src_value.\"Code\" = ?\r\n"
				  + "       and src_value.\"RevisionID\" = 1\r\n"
				  + "       and src_value.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "inner join PIM_MAIN.\"LookupValueReference\" ref\r\n"
				  + "        on ref.\"LookupValueRevisionID\" = src_value.\"ID\"\r\n"
				  + "       and ref.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "inner join PIM_MAIN.\"LookupRevision\" target_lookup\r\n"
				  + "        on target_lookup.\"LookupID\" = ref.\"RefLookupID\"\r\n"
				  + "       and target_lookup.\"Identifier\" = ?\r\n"
				  + "       and target_lookup.\"RevisionID\" = 1\r\n"
				  + "       and target_lookup.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "inner join PIM_MAIN.\"LookupValueRevision\" target_value\r\n"
				  + "        on target_value.\"LookupValueID\" = ref.\"RefLookupValueID\"\r\n"
				  + "       and target_value.\"LookupID\" = ref.\"RefLookupID\"\r\n"
				  + "       and target_value.\"RevisionID\" = 1\r\n"
				  + "       and target_value.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "left join PIM_MAIN.\"LookupValueLang\" target_lang\r\n"
				  + "       on target_lang.\"LookupValueRevisionID\" = target_value.\"ID\"\r\n"
				  + "      and target_lang.\"LanguageID\" = ?\r\n"
				  + "      and target_lang.\"DeletionTimestamp\" =\r\n"
				  + "          timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "where src_lookup.\"Identifier\" = ?\r\n"
				  + "  and src_lookup.\"RevisionID\" = 1\r\n"
				  + "  and src_lookup.\"DeletionTimestamp\" =\r\n"
				  + "      timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "order by target_value.\"Code\""
				;

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, sourceValueCode);
			pstmnt.setNString(2, targetLookupIdentifier);
			pstmnt.setInt(3, languageID);
			pstmnt.setNString(4, sourceLookupIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					values.put(
						rs.getString("ReferencedCode"),
						java.util.Objects.toString(
							rs.getString("ReferencedName"),
							""
						)
					);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}

		return values;
	}
	
	public String getLookupValueName(
			String lookupIdentifier,
			String code,
			int languageID
	) {
		if (lookupIdentifier == null || lookupIdentifier.isBlank()
				|| code == null || code.isBlank()) {
			return null;
		}

		String sql =
				  "select /*+\r\n"
				  + "           qb_name(main_qb)\r\n"
				  + "           leading(lr lvr lvl)\r\n"
				  + "           use_nl(lvr lvl)\r\n"
				  + "           index(lr XAK2_LookupRevision)\r\n"
				  + "           index(lvr XAK2_LookupValueRevision)\r\n"
				  + "           first_rows(1)\r\n"
				  + "       */\r\n"
				  + "       lvl.\"Name\"\r\n"
				  + "from PIM_MAIN.\"LookupRevision\" lr\r\n"
				  + "inner join PIM_MAIN.\"LookupValueRevision\" lvr\r\n"
				  + "        on lvr.\"LookupID\" = lr.\"LookupID\"\r\n"
				  + "       and lvr.\"Code\" = ?\r\n"
				  + "       and lvr.\"RevisionID\" = 1\r\n"
				  + "       and lvr.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "inner join PIM_MAIN.\"LookupValueLang\" lvl\r\n"
				  + "        on lvl.\"LookupValueRevisionID\" = lvr.\"ID\"\r\n"
				  + "       and lvl.\"LanguageID\" = ?\r\n"
				  + "       and lvl.\"DeletionTimestamp\" =\r\n"
				  + "           timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "where lr.\"Identifier\" = ?\r\n"
				  + "  and lr.\"RevisionID\" = 1\r\n"
				  + "  and lr.\"DeletionTimestamp\" =\r\n"
				  + "      timestamp '9999-12-31 00:00:00.0'\r\n"
				  + "  and rownum = 1"
				;

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			pstmnt.setNString(1, code);
			pstmnt.setInt(2, languageID);
			pstmnt.setNString(3, lookupIdentifier);
			pstmnt.setQueryTimeout(30);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				return rs.next()
						? rs.getString("Name")
						: null;
			}
		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
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
	
	public org.json.JSONObject getProductIaGovernanceData(String identifier) {
	    org.json.JSONObject result = new org.json.JSONObject()
	            .put("ProductName", "")
	            .put("ItemGroupIAConfidenceDir", "")
	            .put("ItemGroupIAConfidenceIG", "")
	            .put("ItemGroupIAConfidenceSec", "");

	    if (identifier == null || identifier.trim().isEmpty()) {
	        return result;
	    }

	    handleRefreshConnection();

	    String sql =
	          "select /*+ leading(ar) "
	        + "           use_nl(al dir_cr dir_acv dir_lang "
	        + "                  ig_cr ig_acv ig_lang "
	        + "                  sec_cr sec_acv sec_lang) "
	        + "           index(ar IX_AR_TUNE_01) "
	        + "           index(dir_acv IX_ACV_TUNE_02) "
	        + "           index(ig_acv IX_ACV_TUNE_02) "
	        + "           index(sec_acv IX_ACV_TUNE_02) */ "
	        + "       al.\"Res_Text250_01\" as \"ProductName\", "
	        + "       dir_lang.\"Value\" as \"ItemGroupIAConfidenceDir\", "
	        + "       ig_lang.\"Value\" as \"ItemGroupIAConfidenceIG\", "
	        + "       sec_lang.\"Value\" as \"ItemGroupIAConfidenceSec\" "
	        + "from \"ArticleRevision\" ar "

	        + "left join \"ArticleLang\" al "
	        + "       on al.\"ArticleRevisionID\" = ar.\"ID\" "
	        + "      and al.\"LanguageID\" = 10 "
	        + "      and al.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "

	        + "left join PIM_MAIN.\"CharacteristicRevision\" dir_cr "
	        + "       on dir_cr.\"Identifier\" = 'ItemGroupIAConfidenceDir' "
	        + "      and dir_cr.\"RevisionID\" = 1 "
	        + "      and dir_cr.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValue\" dir_acv "
	        + "       on dir_acv.\"ArticleRevisionID\" = ar.\"ID\" "
	        + "      and dir_acv.\"CharacteristicID\" = dir_cr.\"CharacteristicID\" "
	        + "      and dir_acv.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValueLang\" dir_lang "
	        + "       on dir_lang.\"ArticleCharactValueID\" = dir_acv.\"ID\" "
	        + "      and dir_lang.\"LanguageID\" = -1 "
	        + "      and dir_lang.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "

	        + "left join PIM_MAIN.\"CharacteristicRevision\" ig_cr "
	        + "       on ig_cr.\"Identifier\" = 'ItemGroupIAConfidenceIG' "
	        + "      and ig_cr.\"RevisionID\" = 1 "
	        + "      and ig_cr.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValue\" ig_acv "
	        + "       on ig_acv.\"ArticleRevisionID\" = ar.\"ID\" "
	        + "      and ig_acv.\"CharacteristicID\" = ig_cr.\"CharacteristicID\" "
	        + "      and ig_acv.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValueLang\" ig_lang "
	        + "       on ig_lang.\"ArticleCharactValueID\" = ig_acv.\"ID\" "
	        + "      and ig_lang.\"LanguageID\" = -1 "
	        + "      and ig_lang.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "

	        + "left join PIM_MAIN.\"CharacteristicRevision\" sec_cr "
	        + "       on sec_cr.\"Identifier\" = 'ItemGroupIAConfidenceSec' "
	        + "      and sec_cr.\"RevisionID\" = 1 "
	        + "      and sec_cr.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValue\" sec_acv "
	        + "       on sec_acv.\"ArticleRevisionID\" = ar.\"ID\" "
	        + "      and sec_acv.\"CharacteristicID\" = sec_cr.\"CharacteristicID\" "
	        + "      and sec_acv.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "
	        + "left join \"ArticleCharactValueLang\" sec_lang "
	        + "       on sec_lang.\"ArticleCharactValueID\" = sec_acv.\"ID\" "
	        + "      and sec_lang.\"LanguageID\" = -1 "
	        + "      and sec_lang.\"DeletionTimestamp\" = "
	        + "          timestamp '9999-12-31 00:00:00.0' "

	        + "where ar.\"Identifier\" = ? "
	        + "  and ar.\"EntityID\" = 1100 "
	        + "  and ar.\"RevisionID\" = 1 "
	        + "  and ar.\"DeletionTimestamp\" = "
	        + "      timestamp '9999-12-31 00:00:00.0'";

	    try (java.sql.PreparedStatement pstmnt =
	            connection().prepareStatement(sql)) {

	        pstmnt.setNString(1, identifier);
	        pstmnt.setQueryTimeout(30);

	        try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
	            if (rs.next()) {
	                result
	                    .put(
	                        "ProductName",
	                        java.util.Objects.toString(
	                            rs.getNString("ProductName"), ""))
	                    .put(
	                        "ItemGroupIAConfidenceDir",
	                        java.util.Objects.toString(
	                            rs.getNString("ItemGroupIAConfidenceDir"), ""))
	                    .put(
	                        "ItemGroupIAConfidenceIG",
	                        java.util.Objects.toString(
	                            rs.getNString("ItemGroupIAConfidenceIG"), ""))
	                    .put(
	                        "ItemGroupIAConfidenceSec",
	                        java.util.Objects.toString(
	                            rs.getNString("ItemGroupIAConfidenceSec"), ""));
	            }
	        }
	    } catch (java.sql.SQLException e) {
	        logE(e);
	    }

	    return result;
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
				  + "      AND dd.\"StructureID\" = (select /*+ index(sr XAK1_StructureRevision) */ sr.\"StructureID\" from PIM_MAIN.\"StructureRevision\" sr where sr.\"Identifier\" = N'PrimaryProductTaxonomy' and sr.\"RevisionID\" = 1 and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0') \r\n"
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

//		log("From getArticleData: " + rw.formatTime(System.currentTimeMillis() - init));
	    return productData;
	}
	
	public Integer getLeafStructureGroupId(
			int structureId,
			String parentIdentifier,
			String primaryIdentifier,
			String secondaryIdentifier) {

		if (primaryIdentifier == null || primaryIdentifier.isBlank()) {
			return null;
		}

		handleRefreshConnection();

		boolean hasParent =
				parentIdentifier != null
				&& !parentIdentifier.isBlank();

		boolean hasSecondary =
				secondaryIdentifier != null
				&& !secondaryIdentifier.isBlank();

		String sql =
				  " select bb.\"StructureGroupID\", bb.\"Identifier\" "
				+ " from PIM_MAIN.\"StructureGroupDetail\" bb "
				+ " inner join PIM_MAIN.\"StructureGroupRevision\" aa "
				+ "    on aa.ID = bb.\"StructureGroupRevisionID\" "
				+ "   and aa.\"RevisionID\" = 1 "
				+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and aa.\"StructureID\" = ? "
				+ " where bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
				+ "   and bb.\"NodeType\" = 'leaf' "
				+ (hasParent
						? "   and bb.\"ParentIdentifier\" = ? "
						: "")
				+ (hasSecondary
						? "   and bb.\"Identifier\" in (?, ?) "
						: "   and bb.\"Identifier\" = ? ");

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			int parameterIndex = 1;

			pstmnt.setInt(parameterIndex++, structureId);

			if (hasParent) {
				pstmnt.setString(
						parameterIndex++,
						parentIdentifier);
			}

			pstmnt.setString(
					parameterIndex++,
					primaryIdentifier);

			if (hasSecondary) {
				pstmnt.setString(
						parameterIndex++,
						secondaryIdentifier);
			}

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2);

			Integer secondaryId = null;

			try (java.sql.ResultSet rs =
					pstmnt.executeQuery()) {

				while (rs.next()) {

					String identifier =
							rs.getString("Identifier");

					int structureGroupId =
							rs.getInt("StructureGroupID");

					if (primaryIdentifier.equals(identifier)) {
						return structureGroupId;
					}

					if (hasSecondary
							&& secondaryIdentifier.equals(identifier)) {
						secondaryId = structureGroupId;
					}
				}
			}

			return secondaryId;

		} catch (java.sql.SQLException e) {
			logE(e);
			return null;
		}
	}
	
	/**
	 * Resolves active Article (EntityID 1000) identifiers by SKU in bulk.
	 * The first active article found for a SKU wins, matching the old single-value
	 * semantics used by the STEP processors.
	 */
	public java.util.Map<String, String> getArticlesBySKUs(
			java.util.Collection<String> skus) {

		java.util.Map<String, String> articlesBySKU =
				new java.util.LinkedHashMap<>();

		for (java.util.List<String> chunk : identifierChunks(skus)) {
			java.util.List<Long> numericSKUs = new java.util.ArrayList<>();
			java.util.Map<Long, java.util.List<String>> originalSKUsByNumber =
					new java.util.LinkedHashMap<>();

			for (String sku : chunk) {
				try {
					Long numericSKU = Long.valueOf(sku);
					if (!originalSKUsByNumber.containsKey(numericSKU)) {
						numericSKUs.add(numericSKU);
						originalSKUsByNumber.put(numericSKU, new java.util.ArrayList<String>());
					}
					originalSKUsByNumber.get(numericSKU).add(sku);
				} catch (NumberFormatException e) {
					log("Invalid SKU: " + sku);
				}
			}

			if (numericSKUs.isEmpty()) continue;
			handleRefreshConnection();

			String sql =
					  " select /*+ leading(aa bb) use_nl(bb) index(aa IX_AD_TUNE_01) */ "
					+ "        aa.\"Res_Int_02\" \"SKU\" "
					+ "       ,bb.\"Identifier\" \"ArticleIdentifier\" "
					+ " from \"ArticleDetail\" aa "
					+ " inner join \"ArticleRevision\" bb "
					+ "    on bb.\"ID\" = aa.\"ArticleRevisionID\" "
					+ "   and bb.\"EntityID\" = 1000 "
					+ "   and bb.\"RevisionID\" = 1 "
					+ "   and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ " where aa.\"Res_Int_02\" in (" + placeholders(numericSKUs.size()) + ") "
					+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
				for (int i = 0; i < numericSKUs.size(); i++) {
					pstmnt.setLong(i + 1, numericSKUs.get(i));
				}
				pstmnt.setQueryTimeout(30);
				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						java.util.List<String> originals = originalSKUsByNumber.get(rs.getLong("SKU"));
						if (originals == null) continue;
						for (String original : originals) {
							articlesBySKU.putIfAbsent(original, rs.getString("ArticleIdentifier"));
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return articlesBySKU;
	}


	/**
	 * Returns only the requested StructureGroup identifiers that really exist in
	 * the named structure. It intentionally does not load the entire hierarchy.
	 */
	public java.util.Set<String> getExistingStructureGroupIdentifiers(
			String structureIdentifier,
			java.util.Collection<String> identifiers) {

		java.util.Set<String> result = new java.util.TreeSet<>();
		if (structureIdentifier == null || structureIdentifier.isBlank()) return result;

		for (java.util.List<String> chunk : identifierChunks(identifiers)) {
			if (chunk.isEmpty()) continue;
			handleRefreshConnection();
			String sql =
					  " select /*+ leading(sr sg) use_nl(sg) "
					+ "index(sr XAK1_StructureRevision) index(sg XAK1_StructureGroupRevision) */ "
					+ "        sg.\"Identifier\" "
					+ " from PIM_MAIN.\"StructureRevision\" sr "
					+ " inner join PIM_MAIN.\"StructureGroupRevision\" sg "
					+ "    on sg.\"StructureID\" = sr.\"StructureID\" "
					+ "   and sg.\"RevisionID\" = 1 "
					+ "   and sg.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ " where sr.\"Identifier\" = ? "
					+ "   and sr.\"RevisionID\" = 1 "
					+ "   and sr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ "   and sg.\"Identifier\" in (" + placeholders(chunk.size()) + ")";
			try (java.sql.PreparedStatement pstmnt = connection().prepareStatement(sql)) {
				pstmnt.setNString(1, structureIdentifier);
				bindNStrings(pstmnt, 2, chunk);
				pstmnt.setQueryTimeout(30);
				pstmnt.setFetchSize(2000);
				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String identifier = rs.getString(1);
						if (identifier != null && !identifier.isBlank()) result.add(identifier);
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}
		return result;
	}


	/**
	 * Returns only active, root characteristics from the requested identifiers
	 * whose CharacteristicRevision.Entities contains the requested entity token.
	 *
	 * Entities is tokenized with ';' boundaries so "Article" does not match an
	 * unrelated substring. Example persisted values: "Article;Product2G".
	 */
	public java.util.Set<String> getActiveCharacteristicIdentifiers(
			String entityIdentifier,
			java.util.Collection<String> identifiers) {

		java.util.Set<String> result = new java.util.LinkedHashSet<>();
		if (entityIdentifier == null || entityIdentifier.isBlank()) {
			return result;
		}

		for (java.util.List<String> chunk : identifierChunks(identifiers)) {
			if (chunk.isEmpty()) {
				continue;
			}

			handleRefreshConnection();

			String sql =
					  " select /*+ index(aa XAK2_CharacteristicRevision) */ "
					+ "        aa.\"Identifier\" "
					+ " from PIM_MAIN.\"CharacteristicRevision\" aa "
					+ " where aa.\"Identifier\" in (" + placeholders(chunk.size()) + ") "
					+ "   and aa.\"RevisionID\" = 1 "
					+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
					+ "   and aa.\"IsActive\" = 1 "
					+ "   and aa.\"ParentCharacteristicID\" is null "
					+ "   and aa.\"Identifier\" not like '%\\_Rechazo' escape '\\' "
					+ "   and instr("
					+ "         ';' || replace(nvl(aa.\"Entities\", ''), ' ', '') || ';', "
					+ "         ';' || ? || ';'"
					+ "       ) > 0";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				bindNStrings(pstmnt, 1, chunk);
				pstmnt.setNString(chunk.size() + 1, entityIdentifier);
				pstmnt.setQueryTimeout(30);
				pstmnt.setFetchSize(2000);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String identifier = rs.getString(1);
						if (identifier != null && !identifier.isBlank()) {
							result.add(identifier);
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}

	/**
	 * Resolves the REST object internal-id for ArticleRevision-backed entities.
	 *
	 * P360 List/Object API internal id syntax maps to:
	 *     ArticleRevision.ArticleID + "@" + ArticleRevision.CatalogID
	 *
	 * RevisionID is independent; the API defaults to revision 1 when omitted.
	 */
	public java.util.Map<String, String> getObjectInternalIds(
			int entityID,
			java.util.Collection<String> identifiers) {

		java.util.Map<String, String> result = new java.util.LinkedHashMap<>();

		for (java.util.List<String> chunk : identifierChunks(identifiers)) {
			if (chunk.isEmpty()) {
				continue;
			}

			handleRefreshConnection();

			String sql =
					  " select /*+ index(aa IX_AR_TUNE_01) */ "
					+ "        aa.\"Identifier\" "
					+ "       ,aa.\"ArticleID\" "
					+ "       ,aa.\"CatalogID\" "
					+ " from \"ArticleRevision\" aa "
					+ " where aa.\"Identifier\" in (" + placeholders(chunk.size()) + ") "
					+ "   and aa.\"EntityID\" = ? "
					+ "   and aa.\"CatalogID\" = 1 "
					+ "   and aa.\"RevisionID\" = 1 "
					+ "   and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				bindNStrings(pstmnt, 1, chunk);
				pstmnt.setInt(chunk.size() + 1, entityID);
				pstmnt.setQueryTimeout(30);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String identifier = rs.getString("Identifier");
						if (identifier == null || identifier.isBlank()) {
							continue;
						}
						result.put(
								identifier,
								String.valueOf(rs.getLong("ArticleID"))
										+ "@"
										+ String.valueOf(rs.getInt("CatalogID")));
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}


	/**
	 * Resolves, in reverse, the source lookup-value codes that reference one of the
	 * requested values in another lookup.
	 *
	 * Example:
	 *   sourceLookupIdentifier     = "Characteristics"
	 *   referencedLookupIdentifier = "AttributeGroup"
	 *   referencedValueCodes       = ["CategorySpecificAttributesLVP"]
	 *
	 * The method is intentionally generic; callers decide what the relationship
	 * means for their use case.
	 */
	public java.util.Map<String, java.util.Set<String>> getSourceLookupValueCodesByReferencedLookupValueCodes(
			String sourceLookupIdentifier,
			String referencedLookupIdentifier,
			java.util.Collection<String> referencedValueCodes) {

		java.util.Map<String, java.util.Set<String>> result =
				new java.util.LinkedHashMap<>();
		java.util.List<String> referencedCodes =
				normalizeIdentifiers(referencedValueCodes);

		for (String referencedCode : referencedCodes) {
			result.put(referencedCode, new java.util.LinkedHashSet<String>());
		}

		if (sourceLookupIdentifier == null
				|| sourceLookupIdentifier.isBlank()
				|| referencedLookupIdentifier == null
				|| referencedLookupIdentifier.isBlank()
				|| referencedCodes.isEmpty()) {

			return result;
		}

		handleRefreshConnection();

		for (java.util.List<String> chunk : identifierChunks(referencedCodes)) {
			String sql =
					  " select /*+ "
					+ "     leading(src_lr src_lvr ref target_lr target_lvr) "
					+ "     use_nl(src_lvr ref target_lr target_lvr) "
					+ "     index(src_lr XAK2_LookupRevision) "
					+ "     index(src_lvr XIF2_LookupValueRevision) "
					+ "     index(ref IX_LVREF_LVRID_DELT) "
					+ "     index(target_lr XAK2_LookupRevision) "
					+ "     index(target_lvr XAK1_LookupValueRevision) "
					+ " */ "
					+ "        target_lvr.\"Code\" \"ReferencedCode\" "
					+ "       ,src_lvr.\"Code\" \"SourceCode\" "
					+ " from PIM_MAIN.\"LookupRevision\" src_lr "
					+ " inner join PIM_MAIN.\"LookupValueRevision\" src_lvr "
					+ "    on src_lvr.\"LookupID\" = src_lr.\"LookupID\" "
					+ "   and src_lvr.\"RevisionID\" = 1 "
					+ "   and src_lvr.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ "   and src_lvr.\"IsActive\" = 1 "
					+ " inner join PIM_MAIN.\"LookupValueReference\" ref "
					+ "    on ref.\"LookupValueRevisionID\" = src_lvr.\"ID\" "
					+ "   and ref.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " inner join PIM_MAIN.\"LookupRevision\" target_lr "
					+ "    on target_lr.\"LookupID\" = ref.\"RefLookupID\" "
					+ "   and target_lr.\"Identifier\" = ? "
					+ "   and target_lr.\"RevisionID\" = 1 "
					+ "   and target_lr.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ " inner join PIM_MAIN.\"LookupValueRevision\" target_lvr "
					+ "    on target_lvr.\"LookupID\" = ref.\"RefLookupID\" "
					+ "   and target_lvr.\"LookupValueID\" = ref.\"RefLookupValueID\" "
					+ "   and target_lvr.\"RevisionID\" = 1 "
					+ "   and target_lvr.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ "   and target_lvr.\"IsActive\" = 1 "
					+ " where src_lr.\"Identifier\" = ? "
					+ "   and src_lr.\"RevisionID\" = 1 "
					+ "   and src_lr.\"DeletionTimestamp\" = "
					+ "       timestamp '9999-12-31 00:00:00.0' "
					+ "   and target_lvr.\"Code\" in ("
					+ placeholders(chunk.size())
					+ ") "
					+ " order by target_lvr.\"Code\", src_lvr.\"Code\"";

			try (java.sql.PreparedStatement pstmnt =
					connection().prepareStatement(sql)) {

				int parameterIndex = 1;
				pstmnt.setNString(
						parameterIndex++,
						referencedLookupIdentifier);
				pstmnt.setNString(
						parameterIndex++,
						sourceLookupIdentifier);
				bindNStrings(pstmnt, parameterIndex, chunk);
				pstmnt.setQueryTimeout(30);
				pstmnt.setFetchSize(1000);

				try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
					while (rs.next()) {
						String referencedCode =
								rs.getString("ReferencedCode");
						String sourceCode =
								rs.getString("SourceCode");

						java.util.Set<String> values =
								result.get(referencedCode);

						if (values != null
								&& sourceCode != null
								&& !sourceCode.isBlank()) {

							values.add(sourceCode);
						}
					}
				}
			} catch (java.sql.SQLException e) {
				logE(e);
			}
		}

		return result;
	}


	/**
	 * Loads the active P360 data graph rooted in ArticleRevision for a single
	 * identifier. The returned structure is deliberately table-oriented:
	 *
	 *   revision
	 *   detail
	 *   detailLookups
	 *   languages[]
	 *   domains[]
	 *   structures[]
	 *   characteristics[]
	 *
	 * No business meaning is assigned here to generic columns such as Res_Int_01.
	 * That interpretation belongs to the caller.
	 */
	public org.json.JSONObject getEntityData(
			int entityID,
			String identifier) {

		if (identifier == null || identifier.isBlank()) {
			return new org.json.JSONObject();
		}

		java.util.Map<String, org.json.JSONObject> rows =
				getEntityData(
						entityID,
						java.util.Collections.singletonList(identifier),
						10);

		org.json.JSONObject value = rows.get(identifier);
		return value == null
				? new org.json.JSONObject()
				: value;
	}


	public java.util.Map<String, org.json.JSONObject> getEntityData(
			int entityID,
			java.util.Collection<String> identifiers) {

		return getEntityData(entityID, identifiers, 10);
	}


	/**
	 * Bulk variant of getEntityData. Lookup labels are resolved in the requested
	 * language, while ArticleLang and ArticleCharactValueLang rows themselves are
	 * returned without imposing a language interpretation.
	 */
	public java.util.Map<String, org.json.JSONObject> getEntityData(
			int entityID,
			java.util.Collection<String> identifiers,
			int lookupLanguageID) {

		java.util.Map<String, org.json.JSONObject> result =
				new java.util.LinkedHashMap<>();

		java.util.List<java.util.List<String>> chunks =
				identifierChunks(identifiers);

		if (chunks.isEmpty()) {
			return result;
		}

		handleRefreshConnection();

		for (java.util.List<String> chunk : chunks) {
			for (String identifier : chunk) {
				result.put(
						identifier,
						emptyEntityData(identifier, entityID));
			}

			loadEntityBaseData(
					result,
					entityID,
					chunk,
					lookupLanguageID);

			loadEntityCharacteristicData(
					result,
					entityID,
					chunk,
					lookupLanguageID);
		}

		return result;
	}


	private void loadEntityBaseData(
			java.util.Map<String, org.json.JSONObject> result,
			int entityID,
			java.util.List<String> identifiers,
			int lookupLanguageID) {

		String sql =
				  " select /*+ "
				+ "     leading(ar ad alang dom asm sr) "
				+ "     use_nl(ad alang dom asm sr) "
				+ "     index(ar IX_AR_TUNE_01) "
				+ " */ "
				+ "        ar.\"ID\" \"ArticleRevisionID\" "
				+ "       ,ar.\"ArticleID\" \"ArticleID\" "
				+ "       ,ar.\"Identifier\" \"Identifier\" "
				+ "       ,ar.\"EntityID\" \"EntityID\" "
				+ "       ,ar.\"CatalogID\" \"CatalogID\" "
				+ "       ,ar.\"RevisionID\" \"RevisionID\" "
				+ "       ,ad.\"ID\" \"ArticleDetailID\" "
				+ "       ,ad.\"EAN\" \"AD_EAN\" "
				+ "       ,ad.\"CurrentStatus\" \"AD_CurrentStatus\" "
				+ "       ,ad.\"Res_Int_01\" \"AD_Res_Int_01\" "
				+ "       ,ad.\"Res_Int_02\" \"AD_Res_Int_02\" "
				+ "       ,ad.\"Res_Int_03\" \"AD_Res_Int_03\" "
				+ "       ,ad.\"Res_Int_04\" \"AD_Res_Int_04\" "
				+ "       ,ad.\"Res_DateTime_01\" \"AD_Res_DateTime_01\" "
				+ "       ,ad.\"Res_DateTime_02\" \"AD_Res_DateTime_02\" "
				+ "       ,ad.\"Res_Text250_02\" \"AD_Res_Text250_02\" "
				+ "       ,ad.\"Res_Text2G_02\" \"AD_Res_Text2G_02\" "
				+ "       ,ad.\"Res_Text2G_03\" \"AD_Res_Text2G_03\" "
				+ "       ,ad.\"Res_Text2G_04\" \"AD_Res_Text2G_04\" "
				+ "       ,ad_bus.\"Code\" \"AD_Res_Int_01_Code\" "
				+ "       ,ad_bus_lang.\"Name\" \"AD_Res_Int_01_Name\" "
				+ "       ,ad_ext.\"Code\" \"AD_Res_Int_04_Code\" "
				+ "       ,ad_ext_lang.\"Name\" \"AD_Res_Int_04_Name\" "
				+ "       ,alang.\"ID\" \"ArticleLangID\" "
				+ "       ,alang.\"LanguageID\" \"AL_LanguageID\" "
				+ "       ,alang.\"Res_Text250_01\" \"AL_Res_Text250_01\" "
				+ "       ,alang.\"DescriptionShort\" \"AL_DescriptionShort\" "
				+ "       ,alang.\"DescriptionLong\" \"AL_DescriptionLong\" "
				+ "       ,alang.\"Res_Text2G_01\" \"AL_Res_Text2G_01\" "
				+ "       ,dom.\"ID\" \"ArticleDomainID\" "
				+ "       ,dom.\"EntityID\" \"DOM_EntityID\" "
				+ "       ,dom.\"Res_Int_01\" \"DOM_Res_Int_01\" "
				+ "       ,dom.\"Res_Int_02\" \"DOM_Res_Int_02\" "
				+ "       ,dom.\"Res_Int_03\" \"DOM_Res_Int_03\" "
				+ "       ,dom.\"Res_Int_04\" \"DOM_Res_Int_04\" "
				+ "       ,dom.\"Res_Int_05\" \"DOM_Res_Int_05\" "
				+ "       ,dom.\"Res_Int_06\" \"DOM_Res_Int_06\" "
				+ "       ,dom.\"Res_Int_07\" \"DOM_Res_Int_07\" "
				+ "       ,dom.\"Res_Int_08\" \"DOM_Res_Int_08\" "
				+ "       ,dom.\"Std_Int_10\" \"DOM_Std_Int_10\" "
				+ "       ,dom.\"Res_Text250_01\" \"DOM_Res_Text250_01\" "
				+ "       ,dom_01.\"Code\" \"DOM_Res_Int_01_Code\" "
				+ "       ,dom_01_lang.\"Name\" \"DOM_Res_Int_01_Name\" "
				+ "       ,dom_02.\"Code\" \"DOM_Res_Int_02_Code\" "
				+ "       ,dom_02_lang.\"Name\" \"DOM_Res_Int_02_Name\" "
				+ "       ,dom_03.\"Code\" \"DOM_Res_Int_03_Code\" "
				+ "       ,dom_03_lang.\"Name\" \"DOM_Res_Int_03_Name\" "
				+ "       ,dom_04.\"Code\" \"DOM_Res_Int_04_Code\" "
				+ "       ,dom_04_lang.\"Name\" \"DOM_Res_Int_04_Name\" "
				+ "       ,dom_05.\"Code\" \"DOM_Res_Int_05_Code\" "
				+ "       ,dom_05_lang.\"Name\" \"DOM_Res_Int_05_Name\" "
				+ "       ,dom_06.\"Code\" \"DOM_Res_Int_06_Code\" "
				+ "       ,dom_06_lang.\"Name\" \"DOM_Res_Int_06_Name\" "
				+ "       ,dom_07.\"Code\" \"DOM_Res_Int_07_Code\" "
				+ "       ,dom_07_lang.\"Name\" \"DOM_Res_Int_07_Name\" "
				+ "       ,dom_08.\"Code\" \"DOM_Res_Int_08_Code\" "
				+ "       ,dom_08_lang.\"Name\" \"DOM_Res_Int_08_Name\" "
				+ "       ,dom_std10.\"Code\" \"DOM_Std_Int_10_Code\" "
				+ "       ,dom_std10_lang.\"Name\" \"DOM_Std_Int_10_Name\" "
				+ "       ,asm.\"ID\" \"ArticleStructureMapID\" "
				+ "       ,asm.\"StructureID\" \"ASM_StructureID\" "
				+ "       ,asm.\"StructureGroupIdentifier\" "
				+ "            \"ASM_StructureGroupIdentifier\" "
				+ "       ,sr.\"Identifier\" \"StructureIdentifier\" "
				+ " from \"ArticleRevision\" ar "
				+ " left join \"ArticleDetail\" ad "
				+ "    on ad.\"ArticleRevisionID\" = ar.\"ID\" "
				+ "   and ad.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" ad_bus "
				+ "    on ad_bus.\"LookupValueID\" = ad.\"Res_Int_01\" "
				+ "   and ad_bus.\"RevisionID\" = 1 "
				+ "   and ad_bus.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" ad_bus_lang "
				+ "    on ad_bus_lang.\"LookupValueRevisionID\" = ad_bus.\"ID\" "
				+ "   and ad_bus_lang.\"LanguageID\" = ? "
				+ "   and ad_bus_lang.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" ad_ext "
				+ "    on ad_ext.\"LookupValueID\" = ad.\"Res_Int_04\" "
				+ "   and ad_ext.\"RevisionID\" = 1 "
				+ "   and ad_ext.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" ad_ext_lang "
				+ "    on ad_ext_lang.\"LookupValueRevisionID\" = ad_ext.\"ID\" "
				+ "   and ad_ext_lang.\"LanguageID\" = ? "
				+ "   and ad_ext_lang.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleLang\" alang "
				+ "    on alang.\"ArticleRevisionID\" = ar.\"ID\" "
				+ "   and alang.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleDomain\" dom "
				+ "    on dom.\"ArticleRevisionID\" = ar.\"ID\" "
				+ "   and dom.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ lookupJoinSql("dom_01", "dom_01_lang", "dom.\"Res_Int_01\"")
				+ lookupJoinSql("dom_02", "dom_02_lang", "dom.\"Res_Int_02\"")
				+ lookupJoinSql("dom_03", "dom_03_lang", "dom.\"Res_Int_03\"")
				+ lookupJoinSql("dom_04", "dom_04_lang", "dom.\"Res_Int_04\"")
				+ lookupJoinSql("dom_05", "dom_05_lang", "dom.\"Res_Int_05\"")
				+ lookupJoinSql("dom_06", "dom_06_lang", "dom.\"Res_Int_06\"")
				+ lookupJoinSql("dom_07", "dom_07_lang", "dom.\"Res_Int_07\"")
				+ lookupJoinSql("dom_08", "dom_08_lang", "dom.\"Res_Int_08\"")
				+ lookupJoinSql("dom_std10", "dom_std10_lang", "dom.\"Std_Int_10\"")
				+ " left join \"ArticleStructureMap\" asm "
				+ "    on asm.\"ArticleRevisionID\" = ar.\"ID\" "
				+ "   and asm.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"StructureRevision\" sr "
				+ "    on sr.\"StructureID\" = asm.\"StructureID\" "
				+ "   and sr.\"RevisionID\" = 1 "
				+ "   and sr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where ar.\"Identifier\" in ("
				+ placeholders(identifiers.size())
				+ ") "
				+ "   and ar.\"EntityID\" = ? "
				+ "   and ar.\"RevisionID\" = 1 "
				+ "   and ar.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by ar.\"Identifier\", alang.\"LanguageID\", "
				+ "          dom.\"ID\", asm.\"ID\"";

		java.util.Map<String, java.util.Set<Long>> languagesSeen =
				new java.util.LinkedHashMap<>();
		java.util.Map<String, java.util.Set<Long>> domainsSeen =
				new java.util.LinkedHashMap<>();
		java.util.Map<String, java.util.Set<Long>> structuresSeen =
				new java.util.LinkedHashMap<>();

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			int parameterIndex = 1;

			pstmnt.setInt(parameterIndex++, lookupLanguageID);
			pstmnt.setInt(parameterIndex++, lookupLanguageID);

			// lookupJoinSql() adds one LookupValueLang bind per domain lookup.
			for (int i = 0; i < 9; i++) {
				pstmnt.setInt(parameterIndex++, lookupLanguageID);
			}

			bindNStrings(pstmnt, parameterIndex, identifiers);
			parameterIndex += identifiers.size();
			pstmnt.setInt(parameterIndex, entityID);

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(1000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getString("Identifier");
					org.json.JSONObject item = result.get(identifier);
					if (item == null) {
						item = emptyEntityData(identifier, entityID);
						result.put(identifier, item);
					}

					item.put("found", true);
					fillRevision(item.getJSONObject("revision"), rs);
					fillDetail(
							item.getJSONObject("detail"),
							item.getJSONObject("detailLookups"),
							rs);

					java.util.Set<Long> itemLanguages =
							languagesSeen.computeIfAbsent(
									identifier,
									k -> new java.util.LinkedHashSet<Long>());
					appendLanguage(
							item.getJSONArray("languages"),
							itemLanguages,
							rs);

					java.util.Set<Long> itemDomains =
							domainsSeen.computeIfAbsent(
									identifier,
									k -> new java.util.LinkedHashSet<Long>());
					appendDomain(
							item.getJSONArray("domains"),
							itemDomains,
							rs);

					java.util.Set<Long> itemStructures =
							structuresSeen.computeIfAbsent(
									identifier,
									k -> new java.util.LinkedHashSet<Long>());
					appendStructure(
							item.getJSONArray("structures"),
							itemStructures,
							rs);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
	}


	private String lookupJoinSql(
			String valueAlias,
			String languageAlias,
			String expression) {

		return " left join PIM_MAIN.\"LookupValueRevision\" " + valueAlias
				+ "    on " + valueAlias + ".\"LookupValueID\" = "
				+ expression
				+ "   and " + valueAlias + ".\"RevisionID\" = 1 "
				+ "   and " + valueAlias + ".\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" " + languageAlias
				+ "    on " + languageAlias
				+ ".\"LookupValueRevisionID\" = "
				+ valueAlias + ".\"ID\" "
				+ "   and " + languageAlias + ".\"LanguageID\" = ? "
				+ "   and " + languageAlias + ".\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' ";
	}


	private void loadEntityCharacteristicData(
			java.util.Map<String, org.json.JSONObject> result,
			int entityID,
			java.util.List<String> identifiers,
			int lookupLanguageID) {

		String sql =
				  " select /*+ "
				+ "     leading(ar acv acvl cr base_lvr base_lvl lang_lvr lang_lvl) "
				+ "     use_nl(acv acvl cr base_lvr base_lvl lang_lvr lang_lvl) "
				+ "     index(ar IX_AR_TUNE_01) "
				+ "     index(acv IX_ACV_TUNE_02) "
				+ "     index(acvl XIF1_ArticleCharactValueLang) "
				+ " */ "
				+ "        ar.\"Identifier\" \"Identifier\" "
				+ "       ,ar.\"ID\" \"ArticleRevisionID\" "
				+ "       ,acv.\"ID\" \"ACV_ID\" "
				+ "       ,acv.\"CharacteristicID\" \"ACV_CharacteristicID\" "
				+ "       ,acv.\"EntityID\" \"ACV_EntityID\" "
				+ "       ,acv.\"RecordKey\" \"ACV_RecordKey\" "
				+ "       ,acv.\"ParentRecordKey\" \"ACV_ParentRecordKey\" "
				+ "       ,acv.\"RootCharacteristicID\" "
				+ "            \"ACV_RootCharacteristicID\" "
				+ "       ,acv.\"Order\" \"ACV_Order\" "
				+ "       ,acv.\"Value\" \"ACV_Value\" "
				+ "       ,acv.\"LookupValueID\" \"ACV_LookupValueID\" "
				+ "       ,cr.\"Identifier\" \"CharacteristicIdentifier\" "
				+ "       ,cr.\"DataType\" \"CharacteristicDataType\" "
				+ "       ,cr.\"LookupID\" \"CharacteristicLookupID\" "
				+ "       ,cr.\"IsMultiValue\" \"CharacteristicIsMultiValue\" "
				+ "       ,cr.\"ParentCharacteristicID\" "
				+ "            \"CharacteristicParentID\" "
				+ "       ,cr.\"Entities\" \"CharacteristicEntities\" "
				+ "       ,base_lvr.\"Code\" \"ACV_LookupCode\" "
				+ "       ,base_lvl.\"Name\" \"ACV_LookupName\" "
				+ "       ,acvl.\"ID\" \"ACVL_ID\" "
				+ "       ,acvl.\"EntityID\" \"ACVL_EntityID\" "
				+ "       ,acvl.\"LanguageID\" \"ACVL_LanguageID\" "
				+ "       ,acvl.\"Value\" \"ACVL_Value\" "
				+ "       ,acvl.\"LookupValueID\" \"ACVL_LookupValueID\" "
				+ "       ,lang_lvr.\"Code\" \"ACVL_LookupCode\" "
				+ "       ,lang_lvl.\"Name\" \"ACVL_LookupName\" "
				+ " from \"ArticleRevision\" ar "
				+ " inner join \"ArticleCharactValue\" acv "
				+ "    on acv.\"ArticleRevisionID\" = ar.\"ID\" "
				+ "   and acv.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join \"ArticleCharactValueLang\" acvl "
				+ "    on acvl.\"ArticleCharactValueID\" = acv.\"ID\" "
				+ "   and acvl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " inner join PIM_MAIN.\"CharacteristicRevision\" cr "
				+ "    on cr.\"CharacteristicID\" = acv.\"CharacteristicID\" "
				+ "   and cr.\"RevisionID\" = 1 "
				+ "   and cr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" base_lvr "
				+ "    on base_lvr.\"LookupValueID\" = acv.\"LookupValueID\" "
				+ "   and base_lvr.\"LookupID\" = cr.\"LookupID\" "
				+ "   and base_lvr.\"RevisionID\" = 1 "
				+ "   and base_lvr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" base_lvl "
				+ "    on base_lvl.\"LookupValueRevisionID\" = base_lvr.\"ID\" "
				+ "   and base_lvl.\"LanguageID\" = ? "
				+ "   and base_lvl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueRevision\" lang_lvr "
				+ "    on lang_lvr.\"LookupValueID\" = acvl.\"LookupValueID\" "
				+ "   and lang_lvr.\"LookupID\" = cr.\"LookupID\" "
				+ "   and lang_lvr.\"RevisionID\" = 1 "
				+ "   and lang_lvr.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " left join PIM_MAIN.\"LookupValueLang\" lang_lvl "
				+ "    on lang_lvl.\"LookupValueRevisionID\" = lang_lvr.\"ID\" "
				+ "   and lang_lvl.\"LanguageID\" = ? "
				+ "   and lang_lvl.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " where ar.\"Identifier\" in ("
				+ placeholders(identifiers.size())
				+ ") "
				+ "   and ar.\"EntityID\" = ? "
				+ "   and ar.\"RevisionID\" = 1 "
				+ "   and ar.\"DeletionTimestamp\" = "
				+ "       timestamp '9999-12-31 00:00:00.0' "
				+ " order by ar.\"Identifier\", "
				+ "          acv.\"Order\" nulls first, "
				+ "          acv.\"RecordKey\" nulls first, "
				+ "          acv.\"ParentRecordKey\" nulls first, "
				+ "          acv.\"ID\", acvl.\"ID\"";

		java.util.Map<String, java.util.Map<Long, org.json.JSONObject>>
				characteristicsByItem = new java.util.LinkedHashMap<>();

		try (java.sql.PreparedStatement pstmnt =
				connection().prepareStatement(sql)) {

			int parameterIndex = 1;
			pstmnt.setInt(parameterIndex++, lookupLanguageID);
			pstmnt.setInt(parameterIndex++, lookupLanguageID);
			bindNStrings(pstmnt, parameterIndex, identifiers);
			parameterIndex += identifiers.size();
			pstmnt.setInt(parameterIndex, entityID);

			pstmnt.setQueryTimeout(30);
			pstmnt.setFetchSize(2000);

			try (java.sql.ResultSet rs = pstmnt.executeQuery()) {
				while (rs.next()) {
					String identifier = rs.getString("Identifier");
					org.json.JSONObject item = result.get(identifier);
					if (item == null) {
						continue;
					}

					long acvID = rs.getLong("ACV_ID");
					java.util.Map<Long, org.json.JSONObject> byID =
							characteristicsByItem.computeIfAbsent(
									identifier,
									k -> new java.util.LinkedHashMap<Long, org.json.JSONObject>());

					org.json.JSONObject characteristic = byID.get(acvID);
					if (characteristic == null) {
						characteristic = newCharacteristic(rs);
						byID.put(acvID, characteristic);
						item.getJSONArray("characteristics")
								.put(characteristic);
					}

					appendCharacteristicLanguageValue(
							characteristic.getJSONArray("languageValues"),
							rs);
				}
			}
		} catch (java.sql.SQLException e) {
			logE(e);
		}
	}


	private org.json.JSONObject emptyEntityData(
			String identifier,
			int entityID) {

		return new org.json.JSONObject()
				.put("found", false)
				.put(
					"revision",
					new org.json.JSONObject()
						.put("Identifier", identifier)
						.put("EntityID", entityID))
				.put("detail", new org.json.JSONObject())
				.put("detailLookups", new org.json.JSONObject())
				.put("languages", new org.json.JSONArray())
				.put("domains", new org.json.JSONArray())
				.put("structures", new org.json.JSONArray())
				.put("characteristics", new org.json.JSONArray());
	}


	private void fillRevision(
			org.json.JSONObject revision,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		putIfNotNull(revision, "ID", rs.getObject("ArticleRevisionID"));
		putIfNotNull(revision, "ArticleID", rs.getObject("ArticleID"));
		putIfNotNull(revision, "Identifier", rs.getString("Identifier"));
		putIfNotNull(revision, "EntityID", rs.getObject("EntityID"));
		putIfNotNull(revision, "CatalogID", rs.getObject("CatalogID"));
		putIfNotNull(revision, "RevisionID", rs.getObject("RevisionID"));
	}


	private void fillDetail(
			org.json.JSONObject detail,
			org.json.JSONObject lookups,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		if (rs.getObject("ArticleDetailID") == null) {
			return;
		}

		putIfNotNull(detail, "ID", rs.getObject("ArticleDetailID"));
		putIfNotNull(detail, "EAN", rs.getString("AD_EAN"));
		putIfNotNull(detail, "CurrentStatus", rs.getObject("AD_CurrentStatus"));
		putIfNotNull(detail, "Res_Int_01", rs.getObject("AD_Res_Int_01"));
		putIfNotNull(detail, "Res_Int_02", rs.getObject("AD_Res_Int_02"));
		putIfNotNull(detail, "Res_Int_03", rs.getObject("AD_Res_Int_03"));
		putIfNotNull(detail, "Res_Int_04", rs.getObject("AD_Res_Int_04"));
		putIfNotNull(
				detail,
				"Res_DateTime_01",
				timestampString(rs, "AD_Res_DateTime_01"));
		putIfNotNull(
				detail,
				"Res_DateTime_02",
				timestampString(rs, "AD_Res_DateTime_02"));
		putIfNotNull(
				detail,
				"Res_Text250_02",
				rs.getString("AD_Res_Text250_02"));
		putIfNotNull(
				detail,
				"Res_Text2G_02",
				rs.getNString("AD_Res_Text2G_02"));
		putIfNotNull(
				detail,
				"Res_Text2G_03",
				rs.getNString("AD_Res_Text2G_03"));
		putIfNotNull(
				detail,
				"Res_Text2G_04",
				rs.getNString("AD_Res_Text2G_04"));

		putLookup(
				lookups,
				"Res_Int_01",
				rs.getObject("AD_Res_Int_01"),
				rs.getString("AD_Res_Int_01_Code"),
				rs.getString("AD_Res_Int_01_Name"));
		putLookup(
				lookups,
				"Res_Int_04",
				rs.getObject("AD_Res_Int_04"),
				rs.getString("AD_Res_Int_04_Code"),
				rs.getString("AD_Res_Int_04_Name"));
	}


	private void appendLanguage(
			org.json.JSONArray languages,
			java.util.Set<Long> seen,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		Object rawID = rs.getObject("ArticleLangID");
		if (!(rawID instanceof Number)) {
			return;
		}

		long id = ((Number) rawID).longValue();
		if (!seen.add(id)) {
			return;
		}

		org.json.JSONObject language = new org.json.JSONObject();
		putIfNotNull(language, "ID", rawID);
		putIfNotNull(language, "LanguageID", rs.getObject("AL_LanguageID"));
		putIfNotNull(
				language,
				"Res_Text250_01",
				rs.getNString("AL_Res_Text250_01"));
		putIfNotNull(
				language,
				"DescriptionShort",
				rs.getNString("AL_DescriptionShort"));
		putIfNotNull(
				language,
				"DescriptionLong",
				rs.getNString("AL_DescriptionLong"));
		putIfNotNull(
				language,
				"Res_Text2G_01",
				rs.getNString("AL_Res_Text2G_01"));

		languages.put(language);
	}


	private void appendDomain(
			org.json.JSONArray domains,
			java.util.Set<Long> seen,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		Object rawID = rs.getObject("ArticleDomainID");
		if (!(rawID instanceof Number)) {
			return;
		}

		long id = ((Number) rawID).longValue();
		if (!seen.add(id)) {
			return;
		}

		org.json.JSONObject domain = new org.json.JSONObject();
		org.json.JSONObject lookups = new org.json.JSONObject();

		putIfNotNull(domain, "ID", rawID);
		putIfNotNull(domain, "EntityID", rs.getObject("DOM_EntityID"));
		putIfNotNull(domain, "Res_Int_01", rs.getObject("DOM_Res_Int_01"));
		putIfNotNull(domain, "Res_Int_02", rs.getObject("DOM_Res_Int_02"));
		putIfNotNull(domain, "Res_Int_03", rs.getObject("DOM_Res_Int_03"));
		putIfNotNull(domain, "Res_Int_04", rs.getObject("DOM_Res_Int_04"));
		putIfNotNull(domain, "Res_Int_05", rs.getObject("DOM_Res_Int_05"));
		putIfNotNull(domain, "Res_Int_06", rs.getObject("DOM_Res_Int_06"));
		putIfNotNull(domain, "Res_Int_07", rs.getObject("DOM_Res_Int_07"));
		putIfNotNull(domain, "Res_Int_08", rs.getObject("DOM_Res_Int_08"));
		putIfNotNull(domain, "Std_Int_10", rs.getObject("DOM_Std_Int_10"));
		putIfNotNull(
				domain,
				"Res_Text250_01",
				rs.getNString("DOM_Res_Text250_01"));

		putDomainLookup(lookups, rs, "Res_Int_01");
		putDomainLookup(lookups, rs, "Res_Int_02");
		putDomainLookup(lookups, rs, "Res_Int_03");
		putDomainLookup(lookups, rs, "Res_Int_04");
		putDomainLookup(lookups, rs, "Res_Int_05");
		putDomainLookup(lookups, rs, "Res_Int_06");
		putDomainLookup(lookups, rs, "Res_Int_07");
		putDomainLookup(lookups, rs, "Res_Int_08");
		putDomainLookup(lookups, rs, "Std_Int_10");

		domain.put("lookups", lookups);
		domains.put(domain);
	}


	private void putDomainLookup(
			org.json.JSONObject lookups,
			java.sql.ResultSet rs,
			String column) throws java.sql.SQLException {

		putLookup(
				lookups,
				column,
				rs.getObject("DOM_" + column),
				rs.getString("DOM_" + column + "_Code"),
				rs.getString("DOM_" + column + "_Name"));
	}


	private void appendStructure(
			org.json.JSONArray structures,
			java.util.Set<Long> seen,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		Object rawID = rs.getObject("ArticleStructureMapID");
		if (!(rawID instanceof Number)) {
			return;
		}

		long id = ((Number) rawID).longValue();
		if (!seen.add(id)) {
			return;
		}

		org.json.JSONObject structure = new org.json.JSONObject();
		putIfNotNull(structure, "ID", rawID);
		putIfNotNull(
				structure,
				"StructureID",
				rs.getObject("ASM_StructureID"));
		putIfNotNull(
				structure,
				"StructureIdentifier",
				rs.getString("StructureIdentifier"));
		putIfNotNull(
				structure,
				"StructureGroupIdentifier",
				rs.getString("ASM_StructureGroupIdentifier"));

		structures.put(structure);
	}


	private org.json.JSONObject newCharacteristic(
			java.sql.ResultSet rs) throws java.sql.SQLException {

		org.json.JSONObject characteristic = new org.json.JSONObject();

		putIfNotNull(characteristic, "ID", rs.getObject("ACV_ID"));
		putIfNotNull(
				characteristic,
				"ArticleRevisionID",
				rs.getObject("ArticleRevisionID"));
		putIfNotNull(
				characteristic,
				"CharacteristicID",
				rs.getObject("ACV_CharacteristicID"));
		putIfNotNull(
				characteristic,
				"Identifier",
				rs.getString("CharacteristicIdentifier"));
		putIfNotNull(
				characteristic,
				"EntityID",
				rs.getObject("ACV_EntityID"));
		putIfNotNull(
				characteristic,
				"RecordKey",
				rs.getString("ACV_RecordKey"));
		putIfNotNull(
				characteristic,
				"ParentRecordKey",
				rs.getString("ACV_ParentRecordKey"));
		putIfNotNull(
				characteristic,
				"RootCharacteristicID",
				rs.getObject("ACV_RootCharacteristicID"));
		putIfNotNull(
				characteristic,
				"Order",
				rs.getObject("ACV_Order"));
		putIfNotNull(
				characteristic,
				"Value",
				rs.getNString("ACV_Value"));
		putIfNotNull(
				characteristic,
				"LookupValueID",
				rs.getObject("ACV_LookupValueID"));
		putIfNotNull(
				characteristic,
				"DataType",
				rs.getString("CharacteristicDataType"));
		putIfNotNull(
				characteristic,
				"LookupID",
				rs.getObject("CharacteristicLookupID"));
		putIfNotNull(
				characteristic,
				"IsMultiValue",
				rs.getObject("CharacteristicIsMultiValue"));
		putIfNotNull(
				characteristic,
				"ParentCharacteristicID",
				rs.getObject("CharacteristicParentID"));
		putIfNotNull(
				characteristic,
				"Entities",
				rs.getString("CharacteristicEntities"));

		Object lookupValueID = rs.getObject("ACV_LookupValueID");
		String lookupCode = rs.getString("ACV_LookupCode");
		String lookupName = rs.getString("ACV_LookupName");
		if (lookupValueID != null
				|| (lookupCode != null && !lookupCode.isBlank())
				|| (lookupName != null && !lookupName.isBlank())) {

			org.json.JSONObject lookup = new org.json.JSONObject();
			if (lookupValueID != null) {
				lookup.put("LookupValueID", lookupValueID);
			}
			if (lookupCode != null && !lookupCode.isBlank()) {
				lookup.put("Code", lookupCode);
			}
			if (lookupName != null && !lookupName.isBlank()) {
				lookup.put("Name", lookupName);
			}
			characteristic.put("lookup", lookup);
		}

		characteristic.put("languageValues", new org.json.JSONArray());
		return characteristic;
	}


	private void appendCharacteristicLanguageValue(
			org.json.JSONArray languageValues,
			java.sql.ResultSet rs) throws java.sql.SQLException {

		Object rawID = rs.getObject("ACVL_ID");
		if (!(rawID instanceof Number)) {
			return;
		}

		long id = ((Number) rawID).longValue();
		for (int i = 0; i < languageValues.length(); i++) {
			org.json.JSONObject existing =
					languageValues.optJSONObject(i);
			if (existing != null
					&& existing.optLong("ID", Long.MIN_VALUE) == id) {

				return;
			}
		}

		org.json.JSONObject languageValue = new org.json.JSONObject();
		putIfNotNull(languageValue, "ID", rawID);
		putIfNotNull(
				languageValue,
				"EntityID",
				rs.getObject("ACVL_EntityID"));
		putIfNotNull(
				languageValue,
				"LanguageID",
				rs.getObject("ACVL_LanguageID"));
		putIfNotNull(
				languageValue,
				"Value",
				rs.getNString("ACVL_Value"));
		putIfNotNull(
				languageValue,
				"LookupValueID",
				rs.getObject("ACVL_LookupValueID"));

		org.json.JSONObject lookup = new org.json.JSONObject();
		Object lookupValueID = rs.getObject("ACVL_LookupValueID");
		String code = rs.getString("ACVL_LookupCode");
		String name = rs.getString("ACVL_LookupName");

		if (lookupValueID != null) {
			lookup.put("LookupValueID", lookupValueID);
		}
		if (code != null && !code.isBlank()) {
			lookup.put("Code", code);
		}
		if (name != null && !name.isBlank()) {
			lookup.put("Name", name);
		}
		if (lookup.length() > 0) {
			languageValue.put("lookup", lookup);
		}

		languageValues.put(languageValue);
	}


	private void putLookup(
			org.json.JSONObject target,
			String property,
			Object lookupValueID,
			String code,
			String name) {

		if (lookupValueID == null
				&& (code == null || code.isBlank())
				&& (name == null || name.isBlank())) {

			return;
		}

		org.json.JSONObject lookup = new org.json.JSONObject();
		if (lookupValueID != null) {
			lookup.put("LookupValueID", lookupValueID);
		}
		if (code != null && !code.isBlank()) {
			lookup.put("Code", code);
		}
		if (name != null && !name.isBlank()) {
			lookup.put("Name", name);
		}

		target.put(property, lookup);
	}


	private void putIfNotNull(
			org.json.JSONObject target,
			String property,
			Object value) {

		if (value != null) {
			target.put(property, value);
		}
	}


	private String timestampString(
			java.sql.ResultSet rs,
			String column) throws java.sql.SQLException {

		java.sql.Timestamp value = rs.getTimestamp(column);
		return value == null ? null : value.toString();
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