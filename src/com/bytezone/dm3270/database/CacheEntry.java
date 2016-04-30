package com.bytezone.dm3270.database;

import java.util.Map;
import java.util.TreeMap;

public class CacheEntry
{
  Dataset dataset;
  Map<String, Member> members;

  public CacheEntry (Dataset dataset)
  {
    this.dataset = dataset;
  }

  public void addMember (Member member)
  {
    //    assert dataset.isPartitioned ();

    if (members == null)
    {
      members = new TreeMap<String, Member> ();
      members.put (member.getName (), member);
    }
    else
    {
      Member oldMember = members.get (member.getName ());
      if (oldMember != null)
        member.merge (oldMember);
      members.put (member.getName (), member);
    }
  }

  public void putMember (Member member)
  {
    if (members == null)
      members = new TreeMap<String, Member> ();
    members.put (member.getName (), member);
  }

  public void replace (Dataset dataset)
  {
    assert dataset.getName ().equals (dataset.getName ());
    this.dataset = dataset;
  }
}