extern crate proc_macro;
use proc_macro::TokenStream;
use quote::quote;
use syn::export::ToTokens;
use syn::{Path, parse_macro_input, Ident, DeriveInput};

struct Message {
    event_type: Ident,

}

trait Summary {
    fn summarize(&self);
}

impl Summary for Path {
    fn summarize(&self) {
        let idents = self.segments.iter().map(|v| v.ident.clone()).collect::<Vec<syn::Ident>>();
        assert!(idents.len() == 2, "Event Not Valid!");
        // match idents.first().unwrap().name {

        // }
        eprintln!("{:#?}", idents.first().unwrap());
    }
}

#[proc_macro_attribute]
pub fn owner(_attr: TokenStream, item: TokenStream) -> TokenStream {
    let mut input = parse_macro_input!(item as syn::ItemFn);
    input.block.stmts.insert(
        0,
        syn::parse(
            quote! {
                self.has_permission();
            }
            .into(),
        )
        .unwrap(),
    );
    let token_stream = quote! {
        #input
    };
    token_stream.into()
}

#[proc_macro]
pub fn emit(item: TokenStream) -> TokenStream {
    let mut input = parse_macro_input!(item as syn::ExprStruct);
    // eprintln!("{:#?}", input.path.summarize());
    let token_stream = quote! {
        //assert!(false, "{:?}", #length);
        #input
    };
    token_stream.into()
}

#[proc_macro_derive(Encode)]
pub fn encode_derive(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let token_stream = quote! {
        //assert!(false, "{:?}", #length);
        #input
    };
    token_stream.into()
}

#[proc_macro_derive(Decode)]
pub fn decode_derive(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let token_stream = quote! {
        //assert!(false, "{:?}", #length);
        #input
    };
    token_stream.into()
}