

package com.laytonsmith.core.functions;

import com.laytonsmith.annotations.api;
import com.laytonsmith.core.*;
import com.laytonsmith.core.constructs.*;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Layton
 */
public class Regex {
    
    public static String docs(){
        return "This class provides regular expression functions. For more details, please see the page on "
                + "[[CommandHelper/Regex|regular expressions]]. Note that all the functions are just passthroughs"
                + " to the Java regex mechanism. If you need to set a flag on the regex, where the api calls"
                + " for a pattern, instead send array('pattern', 'flags') where flags is any of i, m, or s."
                + " Alternatively, using the embedded flag system that Java provides is also valid. Named captures are"
				+ " also supported, using Java 7 syntax.";
    }
    
    @api public static class reg_match extends AbstractFunction implements Optimizable {

        public String getName() {
            return "reg_match";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {pattern, subject} Searches for the given pattern, and returns an array with the results. Captures are supported."
                    + " If the pattern is not found anywhere in the subject, an empty array is returned. The indexes of the array"
                    + " follow typical regex fashion; the 0th element is the whole match, and 1-n are the captures specified in"
                    + " the regex.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException};
        }

        public boolean isRestricted() {
            return false;
        }

        
        public CHVersion since() {
            return CHVersion.V3_2_0;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
            Pattern pattern = getPattern(args[0], t);
            String subject = args[1].val();
            CArray ret = CArray.GetAssociativeArray(t);
            Matcher m = pattern.matcher(subject);
            if(m.find()){
                ret.set(0, new CString(m.group(0), t), t);
                for(int i = 1; i <= m.groupCount(); i++){
                    if(m.group(i) == null){
                        ret.set(i, new CNull(t), t);
                    } else {
                        ret.set(i, Static.resolveConstruct(m.group(i), t), t);
                    }
                }
				for(String key : m.namedGroups().keySet()){
					ret.set(key, m.group(key), t);
				}
            }
            return ret;
        }             

        @Override
        public ParseTree optimizeDynamic(Target t, List<ParseTree> children) throws ConfigCompileException, ConfigRuntimeException {
            if(!children.get(0).getData().isDynamic()){
                getPattern(children.get(0).getData(), t);
            }
            return null;
        } 
		
		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CONSTANT_OFFLINE,
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.OPTIMIZE_DYNAMIC,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_match('(\\\\d)(\\\\d)(\\\\d)', 'abc123')"),
				new ExampleScript("Named captures", "reg_match('abc(?<foo>\\\\d+)(xyz)', 'abc123xyz')"),
				new ExampleScript("Named captures with backreferences", "reg_match('abc(?<foo>\\\\d+)def\\\\k<foo>', 'abc123def123')['foo']")
			};
		}
        
    }
    
    @api public static class reg_match_all extends AbstractFunction implements Optimizable {

        public String getName() {
            return "reg_match_all";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {pattern, subject} Searches subject for all matches to the regular expression given in pattern, unlike reg_match,"
                    + " which just returns the first match.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException};
        }

        public boolean isRestricted() {
            return false;
        }

        
        public CHVersion since() {
            return CHVersion.V3_2_0;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
            Pattern pattern = getPattern(args[0], t);
            String subject = args[1].val();
            CArray fret = new CArray(t);
            Matcher m = pattern.matcher(subject);
			int onGroup = 0;			
            while(m.find(onGroup)){
				//Apparently m.namedGroups() resets the matcher, which causes
				//it to get stuck in an infinite loop. So we're just gonna keep
				//track of the the group ourselves.
				onGroup = m.end();
                CArray ret = CArray.GetAssociativeArray(t);
                ret.set(0, new CString(m.group(0), t), t);

                for(int i = 1; i <= m.groupCount(); i++){
                    ret.set(i, new CString(m.group(i), t), t);
                }
				for(String key : m.namedGroups().keySet()){
					ret.set(key, m.group(key), t);
				}
                fret.push(ret);
            }
            return fret;
        }              

        @Override
        public ParseTree optimizeDynamic(Target t, List<ParseTree> children) throws ConfigCompileException, ConfigRuntimeException {
            if(!children.get(0).getData().isDynamic()){
                getPattern(children.get(0).getData(), t);
            }
            return null;
        }
		
		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CONSTANT_OFFLINE,
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.OPTIMIZE_DYNAMIC,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}
		
		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_match_all('(\\\\d{3})', 'abc123456')"),
				new ExampleScript("Named captures", "reg_match_all('abc(?<foo>\\\\d+)(xyz)', 'abc123xyz')[0]['foo']"),
				new ExampleScript("Named captures with backreferences", "reg_match_all('abc(?<foo>\\\\d+)def\\\\k<foo>', 'abc123def123')[0]['foo']")
			};
		}
        
    }
    
    @api public static class reg_replace extends AbstractFunction implements Optimizable {

        public String getName() {
            return "reg_replace";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "string {pattern, replacement, subject} Replaces any occurances of pattern with the replacement in subject."
                    + " Back references are allowed.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException};
        }

        public boolean isRestricted() {
            return false;
        }

        
        public CHVersion since() {
            return CHVersion.V3_2_0;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
            Pattern pattern = getPattern(args[0], t);
            String replacement = args[1].val();
            String subject = args[2].val();
            String ret = "";
            
            try {
            	ret = pattern.matcher(subject).replaceAll(replacement);
            } catch (IndexOutOfBoundsException e) {
            	throw new ConfigRuntimeException("Expecting a regex group at parameter 1 of reg_replace",
            			ExceptionType.FormatException, t);
            }
            
            return new CString(ret, t);
        }           

        @Override
        public ParseTree optimizeDynamic(Target t, List<ParseTree> children) throws ConfigCompileException, ConfigRuntimeException {
			ParseTree data = children.get(0);
            if(!data.getData().isDynamic()){
				String pattern = data.getData().val();
				if(isLiteralRegex(pattern)){
					//We want to replace this with replace()
					//Note the alternative order of arguments
					ParseTree replace = new ParseTree(new CFunction("replace", t), data.getFileOptions());
					replace.addChildAt(0, children.get(2)); //subject -> main
					replace.addChildAt(1, new ParseTree(new CString(getLiteralRegex(pattern), t), replace.getFileOptions())); //pattern -> what
					replace.addChildAt(2, children.get(1)); //replacement -> that
					return replace;
				} else {
					getPattern(data.getData(), t);
				}
            }
            return null;
//            if(!children.get(0).getData().isDynamic()){
//                getPattern(children.get(0).getData(), t);
//            }
//            return null;
        } 
		
		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CONSTANT_OFFLINE,
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.OPTIMIZE_DYNAMIC,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}
		
		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_replace('\\\\d', 'Z', '123abc')"),
				new ExampleScript("Using backreferences", "reg_replace('abc(\\\\d+)', '$1', 'abc123'"),
				new ExampleScript("Using backreferences with named captures", "reg_replace('abc(?<foo>\\\\d+)', '${foo}', 'abc123')")
			};
		}
        
    }
    
    @api public static class reg_split extends AbstractFunction implements Optimizable{

        public String getName() {
            return "reg_split";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "array {pattern, subject, [limit]} Splits a string on the given regex, and returns an array of the parts. If"
                    + " nothing matched, an array with one element, namely the original subject, is returned."
					+ " Limit defaults to infinity, but if set, only"
					+ " that number of splits will occur.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException};
        }

        public boolean isRestricted() {
            return false;
        }

        
        public CHVersion since() {
            return CHVersion.V3_2_0;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
            Pattern pattern = getPattern(args[0], t);
            String subject = args[1].val();
			/**
			 * We use a different indexing notation than Java's regex split. In the case of
			 * 0 for the limit, we will still return an array of length 1, assuming there are actual
			 * splits available. In Java, a split of 0 will return the same as length 1. In our method
			 * though, the limit is the number of splits themselves, so 1 means that the array will be
			 * length 2, as in, there were 1 splits performed. This matches the behavior of split().
			 */
			int limit = Integer.MAX_VALUE - 1;
			if(args.length >= 3){
				limit = Static.getInt32(args[2], t);
			}
            String [] rsplit = pattern.split(subject, limit + 1);
            CArray ret = new CArray(t);
            for(String split : rsplit){
                ret.push(new CString(split, t));
            }
            return ret;
        }
        
        @Override
        public ParseTree optimizeDynamic(Target t, List<ParseTree> children) throws ConfigCompileException, ConfigRuntimeException {
            ParseTree data = children.get(0);
            if(!data.getData().isDynamic()){
				String pattern = data.getData().val();
				if(isLiteralRegex(pattern)){
					//We want to replace this with split()
					ParseTree split = new ParseTree(new CFunction("split", t), data.getFileOptions());
					split.addChildAt(0, new ParseTree(new CString(getLiteralRegex(pattern), t), split.getFileOptions()));
					split.addChildAt(1, children.get(1));
					return split;
				} else {
					getPattern(data.getData(), t);
				}
            }
            return null;
        } 
		
		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.OPTIMIZE_DYNAMIC,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}
		
		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_split('\\\\d', 'a1b2c3')")
			};
		}
        
    }  
    
    @api public static class reg_count extends AbstractFunction implements Optimizable {

        public String getName() {
            return "reg_count";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "int {pattern, subject} Counts the number of occurances in the subject.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException};
        }

        public boolean isRestricted() {
            return false;
        }

        
        public CHVersion since() {
            return CHVersion.V3_2_0;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
            Pattern pattern = getPattern(args[0], t);
            String subject = args[1].val();
            long ret = 0;
            Matcher m = pattern.matcher(subject);
            while(m.find()){
                ret++;
            }
            return new CInt(ret, t);
        }               

        @Override
        public ParseTree optimizeDynamic(Target t, List<ParseTree> children) throws ConfigCompileException, ConfigRuntimeException {
            if(!children.get(0).getData().isDynamic()){
                getPattern(children.get(0).getData(), t);
            }
            return null;
        }
		
		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CONSTANT_OFFLINE,
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.OPTIMIZE_DYNAMIC,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}
        
		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_count('\\\\d', '123abc')")
			};
		}
    }
    
    @api
    public static class reg_escape extends AbstractFunction implements Optimizable{

        public ExceptionType[] thrown() {
            return null;
        }

        public boolean isRestricted() {
            return false;
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            return new CString(java.util.regex.Pattern.quote(args[0].val()), t);
        }

        public String getName() {
            return "reg_escape";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "string {arg} Escapes arg so that it may be used directly in a regular expression, without fear that"
                    + " it will have special meaning; that is, it escapes all special characters. Use this if you need"
                    + " to use user input or similar as a literal search index.";
        }

        public CHVersion since() {
            return CHVersion.V3_3_1;
        }

		@Override
		public Set<OptimizationOption> optimizationOptions() {
			return EnumSet.of(
						OptimizationOption.CONSTANT_OFFLINE,
						OptimizationOption.CACHE_RETURN,
						OptimizationOption.NO_SIDE_EFFECTS
			);
		}
		
		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "reg_escape('\\\\d+')")
			};
		}
        
    }
    
    private static Pattern getPattern(Construct c, Target t) throws ConfigRuntimeException{
        String regex = "";
        int flags = 0;
        String sflags = "";
        if(c instanceof CArray){
            CArray ca = (CArray)c;
            regex = ca.get(0, t).val();
            sflags = ca.get(1, t).val();
            for(int i = 0; i < sflags.length(); i++){
                if(sflags.toLowerCase().charAt(i) == 'i'){
                    flags |= java.util.regex.Pattern.CASE_INSENSITIVE;
                } else if(sflags.toLowerCase().charAt(i) == 'm'){
                    flags |= java.util.regex.Pattern.MULTILINE;
                } else if(sflags.toLowerCase().charAt(i) == 's'){
                    flags |= java.util.regex.Pattern.DOTALL;
                } else {
                    throw new ConfigRuntimeException("Unrecognized flag: " + sflags.toLowerCase().charAt(i), ExceptionType.FormatException, t);
                }
            }
        } else {
            regex = c.val();
        }
        try{
            return Pattern.compile(regex, flags);
        } catch(PatternSyntaxException e){
            throw new ConfigRuntimeException(e.getMessage(), ExceptionType.FormatException, t);
        }
    }
	
	private static boolean isLiteralRegex(String regex){
		//These are the special characters in a regex. If a regex does not contain any of these
		//characters, we can use a faster method in many cases, though the extra overhead of doing
		//this check only makes sense during optimization, not runtime.
		
		//We also are going to check for the special case where the whole regex starts with \Q and ends with \E, which
		//indicates that they did something like: reg_split(reg_escape('literal string'), '') which is an easily
		//optimizable case, but we will have to transform the regex to get the actual split index, but that's up
		//to the function to call getLiteralRegex. If the internal of the regex further contains more \Q or \E identifiers,
		//they are doing something more complex, so we're just gonna forgo optimizing that.
		if(regex.startsWith("\\Q") && regex.endsWith("\\E") 
				&& !regex.substring(2, regex.length() - 2).contains("\\Q") 
				&& !regex.substring(2, regex.length() - 2).contains("\\E")
				){
			return true;
		}
		String chars = "[\\^$.|?*+()";
		for(int i = 0; i < chars.length(); i++){
			if(regex.contains(Character.toString(chars.charAt(i)))){
				return false;
			}
		}
		return true;
	}
	
	private static String getLiteralRegex(String regex){
		if(regex.startsWith("\\Q") && regex.endsWith("\\E") 
				&& !regex.substring(2, regex.length() - 2).contains("\\Q") 
				&& !regex.substring(2, regex.length() - 2).contains("\\E")
				){
			return regex.substring(2, regex.length() - 2);
		} else {
			return regex;
		}		
	}
}
