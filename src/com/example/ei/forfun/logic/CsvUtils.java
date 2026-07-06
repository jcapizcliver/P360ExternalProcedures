package com.example.ei.forfun.logic;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class CsvUtils
{
  private CsvUtils()
  {
  }

  public static List<String> parseLine(String line, char delimiter)
  {
    List<String> values = new ArrayList<>();
    if (line == null)
    {
      return values;
    }

    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++)
    {
      char c = line.charAt(i);

      if (c == '"')
      {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"')
        {
          current.append('"');
          i++;
        }
        else
        {
          inQuotes = !inQuotes;
        }
      }
      else if (c == delimiter && !inQuotes)
      {
        values.add(current.toString());
        current.setLength(0);
      }
      else
      {
        current.append(c);
      }
    }

    values.add(current.toString());
    return values;
  }

  public static void writeRow(Writer writer, List<String> values, char delimiter) throws IOException
  {
    for (int i = 0; i < values.size(); i++)
    {
      if (i > 0)
      {
        writer.write(delimiter);
      }

      writer.write(escape(values.get(i), delimiter));
    }
  }

  private static String escape(String value, char delimiter)
  {
    String safe = value == null ? "" : value;
    boolean needsQuotes = safe.indexOf(delimiter) >= 0
        || safe.indexOf('"') >= 0
        || safe.indexOf('\n') >= 0
        || safe.indexOf('\r') >= 0;

    if (!needsQuotes)
    {
      return safe;
    }

    return "\"" + safe.replace("\"", "\"\"") + "\"";
  }
}